package io.delilaheve.edgl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a DAO builder, used to generate a Table class from an annotated data class.
 *
 * @property properties set of properties used to generate a DAO.
 */
class DaoBuilder(
    private val properties: DaoProperties
) {

    private val fileSpec = FileSpec.builder(
        packageName = properties.packageName,
        fileName = properties.generatedClassName
    )

    private val selectColumns by lazy {
        properties.originProperties
            .joinToString(",·") {
                it.simpleName.asString()
            }
    }

    /**
     * Build the [FileSpec] for this DAO.
     */
    fun build() {
        val typeSpec = TypeSpec.objectBuilder(name = properties.generatedClassName)
            .superclass(Table::class)
            .addProperties(makeColumnDefinitions())
            .addProperty(
                propertySpec = propertySpec(
                    name = "primaryKey",
                    type = Table.PrimaryKey::class.asTypeName(),
                    initializer = "PrimaryKey(${properties.primaryKeyName})",
                    isPrivate = false,
                    isOverride = true
                )
            )
            .addInitializerBlock(
                CodeBlock.builder()
                    .addStatement("transaction {")
                    .indent()
                    .addStatement("SchemaUtils.create(this@${properties.generatedClassName})")
                    .addStatement("SchemaUtils.createMissingTablesAndColumns(this@${properties.generatedClassName})")
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .addFunctions(makeFunctions())
            .build()
        fileSpec.addType(typeSpec)
            .addImport(
                packageName = "org.jetbrains.exposed.sql",
                names = listOf(
                    "SchemaUtils",
                    "insert",
                    "update",
                    "deleteWhere",
                    "selectAll"
                )
            )
            .addImport(
                packageName = "org.jetbrains.exposed.sql.transactions",
                names = listOf("transaction")
            )
            .addImport(
                packageName = "org.jetbrains.exposed.sql.SqlExpressionBuilder",
                names = listOf("eq")
            )
        if (properties.originProperties.any { it.typeAsString() == LocalDateTime::class.simpleName }) {
            fileSpec.addImport(
                packageName = "java.time",
                names = listOf("LocalDateTime")
            )
        }
    }

    /**
     * Write the internally held [FileSpec] with the given [generator] and [dependencies].
     */
    fun writeTo(
        generator: CodeGenerator,
        dependencies: Dependencies
    ) {
        fileSpec.build()
            .writeTo(generator, dependencies)
    }

    private fun makeColumnDefinitions(): List<PropertySpec> {
        val definitions = mutableListOf<PropertySpec>()
        properties.originProperties
            .forEach {
                definitions.add(
                    propertySpec(
                        name = it.simpleName.asString(),
                        type = it.makeColumnTypeName(),
                        initializer = it.makeColumnInitializer()
                    )
                )
            }
        return definitions
    }

    private fun KSPropertyDeclaration.makeColumnTypeName(): TypeName {
        val columnParameterType = when (typeAsString()) {
            "Int" -> typeNameOf<Int>()
            "Long" -> typeNameOf<Long>()
            "UUID" -> typeNameOf<UUID>()
            "String" -> typeNameOf<String>()
            "LocalDateTime" -> typeNameOf<String>()
            "Boolean" -> typeNameOf<Boolean>()
            "List" -> typeNameOf<String>()
            else -> {
                val kClass = annotations.firstOrNull { it.shortName.asString() == TypeMapping::class.simpleName }
                    ?.arguments
                    ?.firstOrNull { it.name?.getShortName() == "storeAs" }
                    ?.value as? KSType
                kClass?.toTypeName() ?: error("Unsupported property type: ${typeAsString()}; resolved kclass of $kClass\"")
            }
        }
        return Column::class.asTypeName()
            .parameterizedBy(columnParameterType)
    }

    private fun KSPropertyDeclaration.makeColumnInitializer(): String {
        val columnType = columnForType(typeAsString())
            .ifEmpty {
                val annotationArgs = annotations.firstOrNull { it.shortName.asString() == TypeMapping::class.simpleName }
                    ?.arguments
                val columnType = annotationArgs?.first { it.name?.getShortName() == "columnType" }
                    ?.value as? String
                if (columnType.isNullOrEmpty()) {
                    val ksType = annotationArgs?.first { it.name?.getShortName() == "storeAs" }
                        ?.value as? KSType
                    columnForType(ksType?.toClassName()?.simpleName.orEmpty())
                } else {
                    columnType
                }
            }
        if (columnType.isEmpty()) {
            error("Unsupported property type: ${typeAsString()}")
        }
        val wantsAutoIncrement = annotations.firstOrNull { ksAnnotation ->
            ksAnnotation.shortName.asString() == PrimaryKey::class.simpleName
        }
            ?.arguments
            ?.firstOrNull()
            ?.value as? Boolean
            ?: false
        val columnSuffix = if (wantsAutoIncrement) {
            ".autoIncrement()"
        } else {
            ""
        }
        return "$columnType(\"${simpleName.asString()}\")$columnSuffix"
    }

    private fun columnForType(typeName: String) = when (typeName) {
        "Int" -> "integer"
        "Long" -> "long"
        "UUID" -> "uuid"
        "String" -> "text"
        "LocalDateTime" -> "text"
        "Boolean" -> "bool"
        "List" -> "text"
        else -> ""
    }

    private fun makeFunctions(): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        functions.apply{
            add(makeSaveFunction())
            add(makeCreateFunction())
            add(makeUpdateFunction())
            add(makeDeleteFunction())
            add(makeGetFunction())
            add(makeGetAllFunction())
            addAll(makeGetByFunctions())
            add(makeTransformFunction())
        }
        return functions
    }

    private fun makeSaveFunction() = FunSpec.builder(name = "save")
        .addParameter(
            name = "rowItem",
            type = properties.classDeclaration
                .typeName()
        )
        .returns(
            returnType = properties.primaryKey
                .typeNameNullable()
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("val result = if (get(rowItem.${properties.primaryKeyName}) == null) {")
                .indent()
                .addStatement("create(rowItem)")
                .unindent()
                .addStatement("} else {")
                .indent()
                .addStatement("update(rowItem)")
                .unindent()
                .addStatement("}")
                .addStatement("return result")
                .build()
        )
        .build()

    private fun makeCreateFunction() = FunSpec.builder("create")
        .addModifiers(KModifier.PRIVATE)
        .addParameter(
            name = "rowItem",
            type = properties.classDeclaration
                .typeName()
        )
        .returns(
            returnType = properties.primaryKey
                .typeNameNullable()
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("val result = transaction {")
                .indent()
                .addStatement("insert {")
                .indent()
                .apply {
                    properties.originProperties
                        .forEach { addStatement(it.makeInsertUpdateStatement()) }
                }
                .unindent()
                .addStatement("}")
                .unindent()
                .addStatement("}")
                .addStatement("return result.resultedValues")
                .indent()
                .addStatement("?.firstOrNull()")
                .addStatement("?.transform()")
                .addStatement("?.${properties.primaryKeyName}")
                .unindent()
                .build()
        )
        .build()

    private fun makeUpdateFunction() = FunSpec.builder("update")
        .addModifiers(KModifier.PRIVATE)
        .addParameter(
            name = "rowItem",
            type = properties.classDeclaration
                .typeName()
        )
        .returns(
            returnType = properties.primaryKey
                .typeName()
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("transaction {")
                .indent()
                .addStatement(
                    "update({ ${properties.primaryKeyName} eq " +
                            "rowItem.${properties.primaryKeyName} }) {"
                )
                .indent()
                .apply {
                    properties.originProperties
                        .forEach { addStatement(it.makeInsertUpdateStatement()) }
                }
                .unindent()
                .addStatement("}")
                .unindent()
                .addStatement("}")
                .addStatement("return rowItem.${properties.primaryKeyName}")
                .build()
        )
        .build()

    private fun KSPropertyDeclaration.makeInsertUpdateStatement() : String {
        val propertyName = simpleName.asString()
        val isDateTime = typeAsString() == LocalDateTime::class.simpleName
        val isList = typeAsString() == List::class.simpleName
        var statement = when {
            isDateTime -> "it[$propertyName] = rowItem.${propertyName}.toString()"
            isList -> "it[$propertyName] = rowItem.${propertyName}.joinToString(\",\")"
            else -> {
                val statement = "it[$propertyName] = rowItem.${propertyName}"
                val annotationStatement = annotations.firstOrNull {
                    it.shortName.asString() == TypeMapping::class.simpleName
                }
                    ?.arguments
                    ?.firstOrNull { it.name?.asString() == "insertStatement" }
                    ?.value as? String
                if (annotationStatement.isNullOrEmpty()) {
                    statement
                } else {
                    annotationStatement.format(statement)
                }
            }
        }
        if (isNullable()) {
            statement += "!!"
        }
        return statement
    }

    private fun makeDeleteFunction() = FunSpec.builder("delete")
        .addParameter(
            name = "rowKey",
            type = properties.primaryKey
                .typeName()
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("transaction {")
                .indent()
                .addStatement("deleteWhere { ${properties.primaryKeyName} eq rowKey }")
                .unindent()
                .addStatement("}")
                .build()
        )
        .build()

    private fun makeGetFunction() = FunSpec.builder("get")
        .addParameter(
            name = "rowKey",
            type = properties.primaryKey
                .typeName()
        )
        .returns(
            returnType = properties.classDeclaration
                .typeNameNullable()
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("val result = transaction {")
                .indent()
                .addStatement("select($selectColumns)")
                .indent()
                .addStatement(".where { ${properties.primaryKeyName}.eq(rowKey) }")
                .addStatement(".firstOrNull()")
                .addStatement("?.transform()")
                .unindent()
                .unindent()
                .addStatement("}")
                .addStatement("return result")
                .build()
        )
        .build()

    private fun makeGetAllFunction() = FunSpec.builder("getAll")
        .returns(
            returnType = List::class.asTypeName()
                .parameterizedBy(properties.classDeclaration.typeName())
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("val result = transaction {")
                .indent()
                .addStatement("selectAll().map { it.transform() }")
                .unindent()
                .addStatement("}")
                .addStatement("return result")
                .build()
        )
        .build()

    private fun makeGetByFunctions() = properties.lookupProperties
        .map {
            val funSuffix = it.simpleName
                .asString()
                .replaceFirstChar { char -> char.uppercaseChar() }
            val eqValue = if (it.typeAsString() == LocalDateTime::class.simpleName) {
                "rowKey.toString()"
            } else {
                "rowKey"
            }
            FunSpec.builder("getBy$funSuffix")
                .addParameter(
                    name = "rowKey",
                    type = it.typeName()
                )
                .returns(
                    returnType = List::class.asTypeName()
                        .parameterizedBy(properties.classDeclaration.typeName())
                )
                .addCode(
                    CodeBlock.builder()
                        .addStatement("val result = transaction {")
                        .indent()
                        .addStatement("select($selectColumns)")
                        .indent()
                        .addStatement(".where { ${it.simpleName.asString()}.eq($eqValue) }")
                        .addStatement(".map { it.transform() }")
                        .unindent()
                        .unindent()
                        .addStatement("}")
                        .addStatement("return result")
                        .build()
                )
                .build()
        }

    private fun makeTransformFunction() = FunSpec.builder("transform")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ResultRow::class)
        .returns(
            returnType = properties.classDeclaration
                .typeName()
        )
        .addCode(
            CodeBlock.builder()
                .addStatement("val result = ${properties.originClassName}(")
                .indent()
                .apply {
                    properties.originProperties
                        .forEachIndexed { index, prop ->
                            val statementSuffix = if(index != properties.originProperties.lastIndex) {
                                ","
                            } else {
                                ""
                            }
                            addStatement("${prop.makeTransformStatement()}$statementSuffix")
                        }
                }
                .unindent()
                .addStatement(")")
                .addStatement("return result")
                .build()
        )
        .build()

    private fun KSPropertyDeclaration.makeTransformStatement(): String {
        val propertyName = simpleName.asString()
        val isDateTime = typeAsString() == LocalDateTime::class.simpleName
        val isList = typeAsString() == List::class.simpleName
        return when {
            isDateTime -> "$propertyName = LocalDateTime.parse(this[$propertyName])"
            isList -> "$propertyName = this[$propertyName].split(\",\")"
            else -> {
                val statement = "$propertyName = this[$propertyName]"
                val annotationStatement = annotations.firstOrNull {
                    it.shortName.asString() == TypeMapping::class.simpleName
                }
                    ?.arguments
                    ?.firstOrNull { it.name?.asString() == "transformStatement" }
                    ?.value as? String
                if (annotationStatement.isNullOrEmpty()) {
                    statement
                } else {
                    val mapper = annotationStatement.format("this[$propertyName]")
                    "$propertyName = $mapper"
                }
            }
        }
    }

}

package io.delilaheve.edgl.dao

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.delilaheve.edgl.dao.ColumnDefiner.makeColumnInitializer
import io.delilaheve.edgl.dao.ColumnDefiner.makeColumnTypeName
import io.delilaheve.edgl.shared.isNullable
import io.delilaheve.edgl.shared.isSerializable
import io.delilaheve.edgl.shared.isSupportedPrimitive
import io.delilaheve.edgl.shared.propertySpec
import io.delilaheve.edgl.shared.typeAsString
import io.delilaheve.edgl.shared.typeName
import io.delilaheve.edgl.shared.typeNameNullable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import java.time.ZonedDateTime

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

    private val dateClassNames
        get() = listOf(LocalDateTime::class.simpleName, ZonedDateTime::class.simpleName)

    private var anySerializable = false

    /**
     * Build the [FileSpec] for this DAO.
     */
    fun build(hideColumns: Boolean) {
        val typeSpec = TypeSpec.objectBuilder(name = properties.generatedClassName)
            .superclass(Table::class)
            .addProperties(makeColumnDefinitions(hideColumns))
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
        if (anySerializable) {
            fileSpec.addImport(
                    packageName = "kotlinx.serialization.json",
                    names = listOf("Json")
                )
                .addImport(
                    packageName = "kotlinx.serialization",
                    names = listOf("encodeToString", "decodeFromString")
                )
        }
        if (properties.originProperties.any { it.typeAsString() == LocalDateTime::class.simpleName }) {
            fileSpec.addImport(
                packageName = "java.time",
                names = listOf("LocalDateTime")
            )
        }
        if (properties.originProperties.any { it.typeAsString() == ZonedDateTime::class.simpleName }) {
            fileSpec.addImport(
                packageName = "java.time",
                names = listOf("ZonedDateTime")
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

    private fun makeColumnDefinitions(
        hideColumns: Boolean
    ): List<PropertySpec> {
        val definitions = mutableListOf<PropertySpec>()
        properties.originProperties
            .forEach {
                definitions.add(
                    propertySpec(
                        name = it.simpleName.asString(),
                        type = it.makeColumnTypeName(),
                        initializer = it.makeColumnInitializer(),
                        isPrivate = hideColumns
                    )
                )
            }
        return definitions
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
        var statement =  if (isSerializable()) {
            anySerializable = true
            "it[$propertyName] = Json.encodeToString(rowItem.${propertyName})"
        } else when(typeAsString()) {
            LocalDateTime::class.simpleName,
            ZonedDateTime::class.simpleName -> "it[$propertyName] = rowItem.${propertyName}.toString()"
            List::class.simpleName -> "it[$propertyName] = rowItem.${propertyName}.joinToString(\",\")"
            Float::class.simpleName -> "it[$propertyName] = rowItem.${propertyName}.toString()"
            else -> when {
                isSupportedPrimitive() -> "it[$propertyName] = rowItem.${propertyName}"
                isSerializable() -> {
                    anySerializable = true
                    "it[$propertyName] = Json.encodeToString(rowItem.${propertyName})"
                }
                else -> error("Unsupported property type: $propertyName; Did you forget a serializer?")
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
                .addStatement("selectAll()")
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
            val eqValue = if (it.typeAsString() in dateClassNames) {
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
                        .apply {
                            if (it.isNullable()) {
                                addStatement("if (rowKey == null) {")
                                indent()
                                addStatement("return emptyList()")
                                unindent()
                                addStatement("}")
                            }
                            addStatement("val result = transaction {")
                            indent()
                            addStatement("selectAll()")
                            indent()
                            addStatement(".where { ${it.simpleName.asString()}.eq($eqValue) }")
                            addStatement(".map { it.transform() }")
                            unindent()
                            unindent()
                            addStatement("}")
                            addStatement("return result")
                        }
                        .build()
                )
                .build()
        }

    private fun makeTransformFunction() = FunSpec.builder("transform")
        .addModifiers(KModifier.INTERNAL)
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
        val propertyType = typeAsString()
        return if (isSerializable()) {
            anySerializable = true
            "$propertyName = Json.decodeFromString(this[$propertyName])"
        } else when (typeAsString()) {
            LocalDateTime::class.simpleName -> "$propertyName = LocalDateTime.parse(this[$propertyName])"
            ZonedDateTime::class.simpleName -> "$propertyName = ZonedDateTime.parse(this[$propertyName])"
            List::class.simpleName -> "$propertyName = this[$propertyName].split(\",\")"
            Float::class.simpleName -> "$propertyName = this[$propertyName].toFloat()"
            else -> when {
                isSupportedPrimitive() -> "$propertyName = this[$propertyName]"
                isSerializable() -> {
                    anySerializable = true
                    "$propertyName = Json.decodeFromString(this[$propertyName])"
                }
                else -> error("Unsupported property type: $propertyName:$propertyType; Did you forget a serializer?")
            }
        }
    }

}
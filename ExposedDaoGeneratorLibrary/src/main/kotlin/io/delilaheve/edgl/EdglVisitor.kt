package io.delilaheve.edgl

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.delilaheve.edgl.data.DaoProperties
import io.delilaheve.edgl.data.PrimaryKeyProperties
import java.io.OutputStream
import java.time.LocalDateTime

class EdglVisitor(
    private val stream: OutputStream,
    private val logger: KSPLogger
) : KSVisitorVoid() {

    companion object {
        private const val USE_POET = true
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.exception(IllegalArgumentException("TableSchema can only be applied to data classes."))
        }
        val info = classDeclaration.getClassInfo()
        val properties = classDeclaration.getAllProperties()
            .filter { it.validate() }
        var hasDeclaredPrimaryKey = false
        var primaryKeyStatement = ""
        var primaryKeyTypeDeclaration = ""
        var columnDeclarations = ""
        val additionalImports = mutableListOf<String>()
        var primaryKeyPropertyName = ""
        val lookupProperties = mutableListOf<KSPropertyDeclaration>()
        properties.forEach {
            val propertyName = it.simpleName.asString()
            if (propertyName == "id") {
                error("'id' is a reserved property name, please use something more descriptive.")
            }
            val type = it.typeAsString()
            var columnType = when (type) {
                "Int" -> "integer"
                "Long" -> "long"
                "UUID" -> {
                    if (!additionalImports.contains("java.util.UUID")) {
                        additionalImports.add("java.util.UUID")
                    }
                    "uuid"
                }
                "String" -> "text"
                "LocalDateTime" -> {
                    if (!additionalImports.contains("java.time.LocalDateTime")) {
                        additionalImports.add("java.time.LocalDateTime")
                    }
                    "text"
                }
                else -> error("Unsupported property type: $type")
            }
            columnType += "(\"$propertyName\")"
            val propertyAnnotation = it.annotations.firstOrNull { ksAnnotation ->
                ksAnnotation.shortName.asString() == PrimaryKey::class.simpleName
            }
            val isLookupKey = it.annotations.any { ksAnnotation ->
                ksAnnotation.shortName.asString() == LookupKey::class.simpleName
            }
            if (isLookupKey) {
                lookupProperties.add(it)
            }
            val wantsPrimaryKey = propertyAnnotation != null
            if (wantsPrimaryKey) {
                if (!hasDeclaredPrimaryKey) {
                    hasDeclaredPrimaryKey = true
                    primaryKeyStatement = "    override val primaryKey = PrimaryKey($propertyName)"
                    primaryKeyTypeDeclaration = type
                    primaryKeyPropertyName = propertyName
                } else {
                    error("Only one primary key may be declared per table.")
                }
            }
            var statement = "    private val $propertyName = $columnType"
            val wantsAutoIncrement = propertyAnnotation?.arguments
                ?.first()
                ?.value as Boolean?
                ?: false
            if (wantsAutoIncrement) {
                statement += ".autoIncrement()"
            }
            columnDeclarations += "$statement\n"
        }
        if (USE_POET) {
            val daoProperties = DaoProperties(
                packageName = info.packageName,
                generatedClassName = info.generatedClassName,
                originClassName = info.className,
                primaryKey = PrimaryKeyProperties(
                    exists = hasDeclaredPrimaryKey,
                    statement = primaryKeyStatement,
                    declaration = primaryKeyTypeDeclaration,
                    propertyName = primaryKeyPropertyName,
                ),
                columnDeclarations = columnDeclarations,
                additionalImports = additionalImports,
                lookupProperties = lookupProperties
            )
            DaoBuilder(properties = daoProperties)
            return
        }
        stream += "package ${info.packageName}"
        stream += ""
        stream += "import org.jetbrains.exposed.sql.*"
        stream += "import org.jetbrains.exposed.sql.transactions.transaction"
        stream += "import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq"
        additionalImports.forEach {
            stream += "import $it"
        }
        stream += ""
        stream += "class ${info.generatedClassName} : Table() {"
        stream += ""
        stream += columnDeclarations
        if (primaryKeyStatement.isNotEmpty()) {
            stream += primaryKeyStatement
        } else {
            error("A primary key must be declared.")
        }
        stream += ""
        stream += "    init {"
        stream += "        SchemaUtils.create(this@${info.generatedClassName})"
        stream += "        SchemaUtils.createMissingTablesAndColumns(this@${info.generatedClassName})"
        stream += "    }"
        stream += ""
        stream += "    fun save(row: ${info.className}) {"
        stream += "        if (get(row.${primaryKeyPropertyName}) == null) {"
        stream += "            create(row)"
        stream += "        } else {"
        stream += "            update(row)"
        stream += "        }"
        stream += "    }"
        stream += ""
        val objAsInsertUpdateStatement = properties.map {
            val propertyName = it.simpleName.asString()
            val isDateTime = it.typeAsString() == LocalDateTime::class.simpleName
            if (isDateTime) {
                "                it[$propertyName] = row.${propertyName}.toString()"
            } else {
                "                it[$propertyName] = row.${propertyName}"
            }
        }.joinToString("\n")
        stream += "    private fun create(row: ${info.className}) {"
        stream += "        transaction {"
        stream += "            insert {"
        stream += objAsInsertUpdateStatement
        stream += "            }"
        stream += "        }"
        stream += "    }"
        stream += ""
        stream += "    private fun update(row: ${info.className}) {"
        stream += "        transaction {"
        stream += "            update({ $primaryKeyPropertyName eq row.${primaryKeyPropertyName} }) {"
        stream += objAsInsertUpdateStatement
        stream += "            }"
        stream += "        }"
        stream += "    }"
        stream += ""
        stream += "    fun delete(rowKey: $primaryKeyTypeDeclaration) {"
        stream += "        transaction {"
        stream += "            deleteWhere { $primaryKeyPropertyName eq rowKey }"
        stream += "        }"
        stream += "    }"
        stream += ""
        stream += "    fun get(rowKey: $primaryKeyTypeDeclaration): ${info.className}? {"
        val selectColumns = properties.map { it.simpleName.asString() }
            .joinToString(", ")
        stream += "        return transaction {"
        stream += "            select($selectColumns).where { $primaryKeyPropertyName.eq(rowKey) }"
        stream += "                .firstOrNull()"
        stream += "                ?.transform()"
        stream += "        }"
        stream += "    }"
        stream += ""
        stream += "    fun getAll(): List<${info.className}> {"
        stream += "        return transaction {"
        stream += "            selectAll().map { it.transform() }"
        stream += "        }"
        stream += "    }"
        stream += ""
        lookupProperties.forEach {
            val funSuffix = it.simpleName
                .asString()
                .replaceFirstChar { char -> char.uppercaseChar() }
            stream += "    fun getBy$funSuffix(rowKey: ${it.typeAsString()}): List<${info.className}> {"
            stream += "        return transaction {"
            val eqValue = if (it.typeAsString() == LocalDateTime::class.simpleName) {
                "rowKey.toString()"
            } else {
                "rowKey"
            }
            stream += "            select($selectColumns).where { ${it.simpleName.asString()}.eq($eqValue) }"
            stream += "                .map { it.transform() }"
            stream += "        }"
            stream += "    }"
        }
        stream += ""
        stream += "    private fun ResultRow.transform(): ${info.className} {"
        stream += "        return ${info.className}("
        properties.forEachIndexed { index, property ->
            val propertyName = property.simpleName.asString()
            val isDateTime = property.typeAsString() == LocalDateTime::class.simpleName
            var statement = if (isDateTime) {
                "$propertyName = LocalDateTime.parse(this[$propertyName])"
            } else {
                "$propertyName = this[$propertyName]"
            }
            if (index != properties.count() - 1) {
                statement += ","
            }
            stream += "            $statement"
        }
        stream += "        )"
        stream += "    }"
        stream += "}"
    }

}

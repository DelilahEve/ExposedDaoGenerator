package io.delilaheve.edgl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Visits [TableSchema] data classes to generate DAOs for the Jetbrains Exposed framework.
 */
class EdgVisitor(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies
) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.CLASS || !classDeclaration.modifiers.contains(Modifier.DATA)) {
            error(
                "TableSchema can only be applied to data classes. " +
                        "${classDeclaration.simpleName.asString()} is not a data class."
            )
        }
        val packageName = classDeclaration.packageName.asString()
        val annotation = classDeclaration.annotations.first { it.shortName.asString() == TableSchema::class.simpleName }
        val dataClassName = classDeclaration.simpleName.asString()
        val generatedClassName = annotation.arguments
            .first { it.name?.asString() == "className" }
            .value as String
        val properties = classDeclaration.getAllProperties()
            .filter { property -> property.validate () && !property.hasAnnotation(NonSavable::class) }
        classDeclaration.asStarProjectedType().toTypeName()
        var hasDeclaredPrimaryKey = false
        val lookupProperties = mutableListOf<KSPropertyDeclaration>()
        var primaryKeyPropertyDeclaration: KSPropertyDeclaration? = null
        properties.forEach {
            val propertyName = it.simpleName.asString()
            if (propertyName == "id") {
                error("'id' in ${classDeclaration.simpleName.asString()} is using a reserved property name.")
            }
            val wantsPrimaryKey = it.hasAnnotation(PrimaryKey::class)
            val isLookupKey = it.hasAnnotation(LookupKey::class)
            if (isLookupKey) {
                lookupProperties.add(it)
            }
            if (wantsPrimaryKey) {
                if (!hasDeclaredPrimaryKey) {
                    hasDeclaredPrimaryKey = true
                    primaryKeyPropertyDeclaration = it
                } else {
                    error(
                        "Only one primary key may be declared per table. " +
                                "${classDeclaration.simpleName.asString()} has multiple."
                    )
                }
            }
        }
        if (primaryKeyPropertyDeclaration == null) {
            error("A primary key must be declared for ${classDeclaration.simpleName.asString()}.")
        } else {
            val daoProperties = DaoProperties(
                packageName = packageName,
                generatedClassName = generatedClassName.ifEmpty { "${dataClassName}Table" },
                originClassName = dataClassName,
                classDeclaration = classDeclaration,
                primaryKey = primaryKeyPropertyDeclaration!!,
                originProperties = properties.toList(),
                lookupProperties = lookupProperties
            )
            DaoBuilder(properties = daoProperties).apply {
                build()
                writeTo(codeGenerator, dependencies)
            }
        }
    }
}

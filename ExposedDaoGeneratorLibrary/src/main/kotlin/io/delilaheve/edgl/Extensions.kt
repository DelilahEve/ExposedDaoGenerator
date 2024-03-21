package io.delilaheve.edgl

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.io.OutputStream

operator fun OutputStream.plusAssign(str: String) {
    this.write("$str\n".toByteArray())
}

fun KSClassDeclaration.getClassInfo(): ClassGenerationInfo {
    val packageName = packageName.asString()
    val annotation = annotations.first { it.shortName.asString() == TableSchema::class.simpleName }
    val dataClassName = simpleName.asString()
    val generatedClassName = annotation.arguments
        .first { it.name?.asString() == "className" }
        .value as String
    return ClassGenerationInfo(
        packageName = packageName,
        generatedClassName =  generatedClassName.ifEmpty { "${dataClassName}Table" },
        className = dataClassName
    )
}

fun KSPropertyDeclaration.typeAsString() = type
    .resolve()
    .declaration
    .simpleName
    .asString()

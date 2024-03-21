package io.delilaheve.edgl.data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

data class DaoProperties(
    val packageName: String,
    val generatedClassName: String,
    val originClassName: String,
    val primaryKey: PrimaryKeyProperties,
    val columnDeclarations: String,
    val additionalImports: List<String>,
    val lookupProperties: MutableList<KSPropertyDeclaration>
)

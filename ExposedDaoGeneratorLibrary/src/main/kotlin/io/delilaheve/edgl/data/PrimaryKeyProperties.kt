package io.delilaheve.edgl.data

data class PrimaryKeyProperties(
    val exists: Boolean,
    val statement: String,
    val declaration: String,
    val propertyName: String
)

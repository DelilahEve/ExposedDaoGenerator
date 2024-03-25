package io.delilaheve.edgl

import java.time.LocalDateTime

@TableSchema
data class AutoIncrementSample(
    @PrimaryKey(autoIncrement = true)
    val intId: Int,
    val title: String,
    val text: String,
    @LookupKey
    val timestamp: LocalDateTime
)

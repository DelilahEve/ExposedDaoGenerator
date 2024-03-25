package io.delilaheve.edgl

import java.time.LocalDateTime
import java.util.UUID

@TableSchema(className = "SampleTable")
data class SampleObject(
    @PrimaryKey
    val uuid: UUID,
    @LookupKey
    val title: String,
    val text: String,
    val count: Long,
    val timestamp: LocalDateTime
)

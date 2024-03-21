package io.delilaheve.edgl

import java.time.LocalDateTime
import java.util.UUID

@TableSchema(className = "SampleTable")
data class SampleObject(
    @PrimaryKey
    val uuid: UUID,
    @LookupKey
    val name: String,
    @LookupKey
    val lastName: String,
    @LookupKey
    val timestamp: LocalDateTime
)

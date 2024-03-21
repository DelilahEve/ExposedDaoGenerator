package io.delilaheve.edgl

import java.util.UUID

@TableSchema(className = "OtherTable")
data class OtherSampleObject(
    @PrimaryKey
    val uuid: UUID,
    val text: String,
    @LookupKey
    val title: String,
    val count: Int
)

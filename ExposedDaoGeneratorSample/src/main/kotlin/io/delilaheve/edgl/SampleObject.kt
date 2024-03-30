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
    val tagLine: String?,
    val tags: List<String>,
    val timestamp: LocalDateTime
) {
    @NonSavable
    val titleWithText: String
        get() = "$title: $text"

}

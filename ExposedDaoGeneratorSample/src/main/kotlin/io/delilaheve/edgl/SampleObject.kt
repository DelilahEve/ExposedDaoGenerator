package io.delilaheve.edgl

import io.delilaheve.edgl.shared.LookupKey
import io.delilaheve.edgl.shared.NonSavable
import io.delilaheve.edgl.shared.PrimaryKey
import io.delilaheve.edgl.shared.TableSchema
import java.time.LocalDateTime
import java.util.UUID

@Suppress("unused") // This is purely for example use
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
    val timestamp: LocalDateTime,
    val someFloat: Float
) {
    @NonSavable
    val titleWithText: String
        get() = "$title: $text"

}

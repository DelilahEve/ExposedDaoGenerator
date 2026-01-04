@file:OptIn(ExperimentalUuidApi::class)

package io.delilaheve.edgl

import io.delilaheve.edgl.shared.LookupKey
import io.delilaheve.edgl.shared.NonSavable
import io.delilaheve.edgl.shared.PrimaryKey
import io.delilaheve.edgl.shared.TableSchema
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Suppress("unused") // This is purely for example use
@TableSchema(className = "SampleTable")
data class SampleObject constructor(
    @PrimaryKey
    val uuid: UUID,
    @LookupKey
    val title: String,
    val text: String,
    val count: Long,
    val tagLine: String?,
    val tags: List<String>,
    val timestamp: LocalDateTime,
    val otherTimestamp: ZonedDateTime,
    val someFloat: Float,
    @LookupKey
    val nullableUuid: UUID?,
    val kotlinUuid: Uuid
) {
    @NonSavable
    val titleWithText: String
        get() = "$title: $text"
}

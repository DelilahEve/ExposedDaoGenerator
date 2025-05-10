package io.delilaheve.edgl

import io.delilaheve.edgl.shared.LookupKey
import io.delilaheve.edgl.shared.PrimaryKey
import io.delilaheve.edgl.shared.TableSchema
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

@TableSchema
data class AutoIncrementSample(
    @PrimaryKey(autoIncrement = true)
    val intId: Int,
    val title: String,
    val text: String,
    val hidden: Boolean,
    @LookupKey
    val timestamp: LocalDateTime,
    @Serializable(with = CustomLongSerializer::class)
    val customLongWrapper: CustomLongWrapper
)

object CustomLongSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long {
        return decoder.decodeString().toLong()
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(value.toString())
    }
}


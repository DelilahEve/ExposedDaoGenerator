package io.delilaheve.edgl

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
    @TypeMapping(
        storeAs = Long::class,
        insertStatement = "%s.value",
        transformStatement = "CustomLongWrapper(value = %s)"
    )
    val customLongWrapper: CustomLongWrapper
)

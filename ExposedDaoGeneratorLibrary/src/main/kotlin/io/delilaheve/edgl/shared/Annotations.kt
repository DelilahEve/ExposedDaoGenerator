package io.delilaheve.edgl.shared

/**
 * Marks a data class as a schema for a table.
 *
 * @param className Optional name override for this table. Leave empty for a default generated name.
 * @param hideColumns Optional override to hide column declarations as private members.
 */
@Target(AnnotationTarget.CLASS)
annotation class TableSchema(
    val className: String = "",
    val hideColumns: Boolean = false
)

/**
 * Marks a property as being the primary key for a table.
 *
 * @param autoIncrement should this key be auto-incremented?
 */
@Target(AnnotationTarget.PROPERTY)
annotation class PrimaryKey(
    val autoIncrement: Boolean = false
)

/**
 * Marks a property as being a lookup key for a table.
 *
 * Properties marked with this will have a getBy function generated for them.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class LookupKey

/**
 * Marks a property as transient/non-savable.
 *
 * Properties marked with this will not have a column generated for them.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class NonSavable

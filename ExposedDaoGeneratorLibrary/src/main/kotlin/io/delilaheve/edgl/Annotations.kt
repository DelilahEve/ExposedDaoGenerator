package io.delilaheve.edgl

/**
 * Marks a data class as a schema for a table.
 *
 * @param className Optional name override for this table. Leave empty for a default generated name.
 * @param suspending Whether transaction functions should be suspending.
 */
@Target(AnnotationTarget.CLASS)
annotation class TableSchema(
    val className: String = "",
    val suspending: Boolean = true
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
 * Properties marked with this will have a get function generated for them.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class LookupKey

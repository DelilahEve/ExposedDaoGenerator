package io.delilaheve.edgl

import kotlin.reflect.KClass

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

/**
 * Provides data about a property that would otherwise be unsupported.
 *
 * @param storeAs KClass type this property should be stored as.
 * @param columnType optional way to specify the column initializer (leave blank to auto-determine).
 * @param insertStatement optional way to provide an insert/update statement mapping.
 *      %s will be formatted with the DAO Gen default statement.
 * @param transformStatement optional way to provide a transform statement. This should convert from [storeAs] to your
 *      object's type. %s will be formatted with the DAO Gen default statement.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class TypeMapping(
    val storeAs: KClass<*>,
    val columnType: String = "",
    val insertStatement: String = "%s",
    val transformStatement: String = "%s"
)

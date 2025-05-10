package io.delilaheve.edgl.dao

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Represents a set of properties used in DAO generation.
 *
 * @property packageName Package name of the origin and generated classes.
 * @property generatedClassName Class name of the generated class.
 * @property originClassName Class name of the origin class.
 * @property classDeclaration Declaration of the origin class.
 * @property primaryKey Property declaration from the origin class of the property to use as a primary key.
 * @property originProperties List of property declarations from the origin class.
 * @property lookupProperties List of property declarations from the origin class to use as lookup keys.
 */
data class DaoProperties(
    val packageName: String,
    val generatedClassName: String,
    val originClassName: String,
    val classDeclaration: KSClassDeclaration,
    val primaryKey: KSPropertyDeclaration,
    val originProperties: List<KSPropertyDeclaration>,
    val lookupProperties: MutableList<KSPropertyDeclaration>
) {

    /**
     * String name of [primaryKey] in the origin class.
     */
    val primaryKeyName: String
        get() = primaryKey.simpleName
            .asString()

}
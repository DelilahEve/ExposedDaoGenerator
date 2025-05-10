package io.delilaheve.edgl.shared

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Get the simple name for this [KSPropertyDeclaration].
 */
fun KSPropertyDeclaration.typeAsString(): String = type
    .resolve()
    .declaration
    .simpleName
    .asString()

/**
 * Check for the [annotationClass] on this [KSPropertyDeclaration].
 */
fun KSPropertyDeclaration.hasAnnotation(annotationClass: KClass<*>): Boolean {
    return annotations.any {
        it.shortName.asString() == annotationClass.simpleName
    }
}

/**
 * Check for the [Serializable] annotation on this [KSPropertyDeclaration].
 */
fun KSPropertyDeclaration.isSerializable(): Boolean {
    return hasAnnotation(Serializable::class)
}

/**
 * Check if this [KSPropertyDeclaration] is declared as a nullable type.
 */
fun KSPropertyDeclaration.isNullable(): Boolean {
    return type.resolve().isMarkedNullable
}

fun KSPropertyDeclaration.isSupportedPrimitive(): Boolean {
    return supportedPrimitives.keys.contains(typeAsString())
}

/**
 * Get the [TypeName] for this [KSPropertyDeclaration].
 */
fun KSPropertyDeclaration.typeName(): TypeName = type.toTypeName()

/**
 * Get the nullable [TypeName] for this [KSPropertyDeclaration].
 */
fun KSPropertyDeclaration.typeNameNullable(): TypeName = type.resolve()
    .makeNullable()
    .toTypeName()

/**
 * Get the [TypeName] for this [KSClassDeclaration].
 */
fun KSClassDeclaration.typeName(): TypeName = asStarProjectedType().toTypeName()

/**
 * Get the nullable [TypeName] for this [KSClassDeclaration].
 */
fun KSClassDeclaration.typeNameNullable(): TypeName = asStarProjectedType().makeNullable()
    .toTypeName()

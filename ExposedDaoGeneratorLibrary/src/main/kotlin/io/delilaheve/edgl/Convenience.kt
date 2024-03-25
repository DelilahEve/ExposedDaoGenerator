package io.delilaheve.edgl

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

/**
 * Create a new [PropertySpec].
 *
 * @param name Property name
 * @param type Property type
 * @param initializer optional initializer
 */
fun propertySpec(
    name: String,
    type: TypeName,
    initializer: String = "",
    isPrivate: Boolean = true,
    isOverride: Boolean = false
): PropertySpec = PropertySpec.builder(name = name, type = type)
    .apply {
        if (initializer.isNotEmpty()) {
            initializer(format = initializer)
        }
        if (isPrivate) {
            addModifiers(KModifier.PRIVATE)
        }
        if (isOverride) {
            addModifiers(KModifier.OVERRIDE)
        }
    }
    .build()

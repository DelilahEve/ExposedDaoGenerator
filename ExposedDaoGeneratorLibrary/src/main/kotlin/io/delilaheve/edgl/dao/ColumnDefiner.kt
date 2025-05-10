package io.delilaheve.edgl.dao

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.typeNameOf
import io.delilaheve.edgl.shared.PrimaryKey
import io.delilaheve.edgl.shared.isSerializable
import io.delilaheve.edgl.shared.supportedPrimitives
import io.delilaheve.edgl.shared.typeAsString
import org.jetbrains.exposed.sql.Column
import java.util.UUID
import kotlin.text.isEmpty

object ColumnDefiner {

    private fun KSPropertyDeclaration.columnType(
        elseBlock: () -> String = {
            if (isSerializable()) {
                "text"
            } else {
                ""
            }
        }
    ): String {
        return supportedPrimitives[typeAsString()] ?: elseBlock()
    }

    fun KSPropertyDeclaration.makeColumnTypeName(): TypeName {
        val columnParameterType = when (typeAsString()) {
            "Int" -> typeNameOf<Int>()
            "Long" -> typeNameOf<Long>()
            "UUID" -> typeNameOf<UUID>()
            "String" -> typeNameOf<String>()
            "LocalDateTime" -> typeNameOf<String>()
            "Boolean" -> typeNameOf<Boolean>()
            "List" -> typeNameOf<String>()
            "Float" -> typeNameOf<String>()
            else -> {
                if (isSerializable()) {
                    typeNameOf<String>()
                } else {
                    error("Unsupported property type: ${typeAsString()}; Did you forget a serializer?")
                }
            }
        }
        return Column::class.asTypeName()
            .parameterizedBy(columnParameterType)
    }

    fun KSPropertyDeclaration.makeColumnInitializer(): String {
        val columnType = columnType()
        if (columnType.isEmpty()) {
            error("Unsupported property type: ${typeAsString()}; Did you forget a serializer?")
        }
        val wantsAutoIncrement = annotations
            .firstOrNull { ksAnnotation ->
                ksAnnotation.shortName.asString() == PrimaryKey::class.simpleName
            }
            ?.arguments
            ?.firstOrNull()
            ?.value as? Boolean
            ?: false
        val columnSuffix = if (wantsAutoIncrement) {
            ".autoIncrement()"
        } else {
            ""
        }
        return "$columnType(\"${simpleName.asString()}\")$columnSuffix"
    }

}

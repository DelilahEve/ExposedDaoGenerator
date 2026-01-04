package io.delilaheve.edgl.dao

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.typeNameOf
import io.delilaheve.edgl.shared.LookupKey
import io.delilaheve.edgl.shared.PrimaryKey
import io.delilaheve.edgl.shared.isNullable
import io.delilaheve.edgl.shared.isSerializable
import io.delilaheve.edgl.shared.supportedPrimitives
import io.delilaheve.edgl.shared.typeAsString
import org.jetbrains.exposed.sql.Column
import java.util.UUID
import kotlin.text.isEmpty
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        return if (isSerializable()) {
            "text"
        } else {
            supportedPrimitives[typeAsString()] ?: elseBlock()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun KSPropertyDeclaration.makeColumnTypeName(): TypeName {
        val columnParameterType = if (isSerializable()) {
            typeNameOf<String>()
        } else {
            when (typeAsString()) {
                "Int" -> typeNameOf<Int>().copy(nullable = isNullable())
                "Long" -> typeNameOf<Long>().copy(nullable = isNullable())
                "UUID" -> typeNameOf<UUID>().copy(nullable = isNullable())
                "Uuid" -> typeNameOf<UUID>().copy(nullable = isNullable())
                "String" -> typeNameOf<String>().copy(nullable = isNullable())
                "LocalDateTime" -> typeNameOf<String>().copy(nullable = isNullable())
                "ZonedDateTime" -> typeNameOf<String>().copy(nullable = isNullable())
                "Boolean" -> typeNameOf<Boolean>().copy(nullable = isNullable())
                "List" -> typeNameOf<String>().copy(nullable = isNullable())
                "Float" -> typeNameOf<String>().copy(nullable = isNullable())
                else -> {
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
        val wantsLookup = annotations
            .firstOrNull {
                it.shortName.asString() == LookupKey::class.simpleName
            } != null
        var columnSuffix = if (wantsAutoIncrement) {
            ".autoIncrement()"
        } else {
            ""
        }
        if (wantsLookup) {
            columnSuffix += ".index()"
        }
        if (isNullable()) {
            columnSuffix += ".nullable()"
        }
        return "$columnType(\"${simpleName.asString()}\")$columnSuffix"
    }

}

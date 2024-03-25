package io.delilaheve.edgl

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

/**
 * Symbol processor for [TableSchema] data classes.
 */
class EdgProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(TableSchema::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
        return if (symbols.iterator().hasNext()) {
            val dependencies = Dependencies(
                aggregating = false,
                sources = resolver.getAllFiles()
                    .toList()
                    .toTypedArray()
            )
            symbols.forEach {
                it.accept(
                    EdgVisitor(codeGenerator, dependencies),
                    Unit
                )
            }
            symbols.filterNot { it.validate() }
                .toList()
        } else {
            emptyList()
        }
    }

}

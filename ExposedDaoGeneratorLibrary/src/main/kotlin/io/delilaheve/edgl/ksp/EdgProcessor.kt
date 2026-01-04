package io.delilaheve.edgl.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.delilaheve.edgl.shared.TableSchema

/**
 * Symbol processor for [TableSchema] data classes.
 */
class EdgProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = resolver.getNewFiles().toSet()
        val symbols = resolver.getSymbolsWithAnnotation(TableSchema::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.containingFile in newFiles }
        return if (symbols.iterator().hasNext()) {
            symbols.forEach {
                val dependencies = Dependencies(
                    aggregating = false,
                    sources = arrayOf(it.containingFile!!)
                )
                it.accept(
                    visitor = EdgVisitor(codeGenerator, dependencies),
                    data = Unit
                )
            }
            symbols.filterNot { it.validate() }
                .toList()
        } else {
            emptyList()
        }
    }

}

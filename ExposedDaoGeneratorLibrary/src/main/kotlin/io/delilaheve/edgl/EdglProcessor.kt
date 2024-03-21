package io.delilaheve.edgl

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

class EdglProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
    private val logger: KSPLogger
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
                val info = it.getClassInfo()
                val stream: OutputStream = codeGenerator.createNewFile(
                    dependencies = dependencies,
                    packageName = info.packageName,
                    fileName = info.generatedClassName
                )
                it.accept(EdglVisitor(stream, logger), Unit)
                stream.close()
            }
            symbols.filterNot { it.validate() }
                .toList()
        } else {
            emptyList()
        }
    }

}

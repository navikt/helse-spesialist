package no.nav.helse.spesialist.api.rest.graphqlgenerator

import java.io.File

private const val OUTPUT_DIR = "../helse-speil/src/io/graphql/rest/spesialist"

fun main() {
    val generator = Generator().apply { generate() }

    generator.queries.forEach { query ->
        val outputPath = "$OUTPUT_DIR/${query.operationName}.query.graphql"
        println("Lagrer query som $outputPath...")
        File(outputPath).writeText(query.toDocument(generator.outputTypes.values))
    }

    generator.mutations.forEach { mutation ->
        val outputPath = "$OUTPUT_DIR/${mutation.operationName}.mutation.graphql"
        println("Lagrer mutation som $outputPath...")
        File(outputPath).writeText(mutation.toDocument(generator.outputTypes.values))
    }

    val outputPath = "$OUTPUT_DIR/schema.graphql"
    println("Lagrer skjema som $outputPath...")
    File(outputPath).writeText(buildString {
        (generator.getReferencedCustomScalarTypes().sortedBy { it.name } +
                generator.enumTypes.values.sortedBy { it.name } +
                generator.inputTypes.values.sortedBy { it.name } +
                generator.outputTypes.values.sortedBy { it.name }).forEach { type ->
            append(type.toSDL() + "\n")
            append("\n")
        }
        append("extend type Query {\n")
        generator.queries.sortedBy { it.fieldName }.forEach { query ->
            append(indentation() + query.toQueryObjectField() + "\n")
        }
        append("}\n")
        append("\n")
        append("extend type Mutation {\n")
        generator.mutations.sortedBy { it.fieldName }.forEach { mutation ->
            append(indentation() + mutation.toMutationObjectField() + "\n")
        }
        append("}\n")
    })
}

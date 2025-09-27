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
        generator.getReferencedCustomScalarTypes().sortedBy(GQLType::name).forEach { scalarType ->
            append(scalarType.toSDL())
            append("\n")
        }
        generator.inputTypes.values.sortedBy { it.name }.forEach { definition ->
            append(definition.toSDL())
            append("\n")
        }
        generator.outputTypes.values.sortedBy { it.name }
            .forEach { definition ->
                append(definition.toSDL())
                append("\n")
            }
        append("extend type Query {\n")
        append(generator.queries.sortedBy { it.fieldName }
            .joinToString("\n    ", prefix = "    ", postfix = "\n") { it.toQueryObjectField() })
        append("}\n")
        append("\n")
        append("extend type Mutation {\n")
        append(generator.mutations.sortedBy { it.fieldName }
            .joinToString("\n    ", prefix = "    ", postfix = "\n") { it.toMutationObjectField() })
        append("}\n")
    })
}

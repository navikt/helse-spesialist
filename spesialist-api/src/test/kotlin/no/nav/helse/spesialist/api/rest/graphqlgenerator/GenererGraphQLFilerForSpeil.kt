package no.nav.helse.spesialist.api.rest.graphqlgenerator

import java.io.File

fun main() {
    val generator = Generator()
    generator.generate()

    val restSchema = buildString {
        generator.inputTypes.values.filterIsInstance<InputObjectTypeDefinition>().sortedBy { it.name }.forEach { definition ->
            append(definition.toSchemaType())
            append("\n")
        }
        generator.outputTypes.values.filterIsInstance<OutputObjectTypeDefinition>().sortedBy { it.name }.forEach { definition ->
            append(definition.toSchemaType())
            append("\n")
        }
        append("extend type Query {\n")
        append(generator.generatedQueries.sorted().joinToString("\n    ", prefix = "    ", postfix = "\n"))
        append("}\n")
        append("\n")
        append("extend type Mutation {\n")
        append(generator.generatedMutations.sorted().joinToString("\n    ", prefix = "    ", postfix = "\n"))
        append("}\n")
    }

    val outputPath = "../helse-speil/src/io/graphql/rest/spesialist/schema.graphql"
    println("Lagrer skjema som $outputPath...")
    File(outputPath).writeText(restSchema)
}

package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentNewlines
import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentation

data class GQLRestQuery(
    val operationName: String,
    val fieldName: String,
    val arguments: Map<String, GQLInputType>,
    val fieldType: GQLOutputType,
    val path: String,
) {
    fun toDocument(allOutputTypes: Collection<GQLOutputType>): String =
        buildString {
            append("query $operationName")
            if (arguments.isNotEmpty()) {
                append("(")
                append(arguments.entries.joinToString(separator = ", ") { (name, type) -> "$$name: ${type.asReference()}" })
                append(")")
            }
            append(" {\n")

            append(indentation() + fieldName)
            if (arguments.isNotEmpty()) {
                append("(")
                append(arguments.keys.joinToString { name -> "$name: $$name" })
                append(")")
            }
            append("\n")

            append(indentation() + "@rest(\n")
            append(indentation(2) + "type: \"${fieldType.asReference()}\"\n")
            append(indentation(2) + "endpoint: \"spesialist\"\n")
            append(indentation(2) + "path: \"$path\"\n")
            append(indentation(2) + "method: \"GET\"\n")
            append(indentation() + ")")
            val selectionSet = fieldType.toSelectionSet(allOutputTypes)
            if (selectionSet.isNotEmpty()) {
                append(" ")
                append(selectionSet.indentNewlines())
            }
            append("\n")
            append("}")
        }

    fun toQueryObjectField(): String =
        buildString {
            append(fieldName)
            if (arguments.isNotEmpty()) {
                append("(")
                append(arguments.entries.joinToString(separator = ", ") { (name, type) -> "$name: ${type.asReference()}" })
                append(")")
            }
            append(": ${fieldType.asReference()}")
        }
}

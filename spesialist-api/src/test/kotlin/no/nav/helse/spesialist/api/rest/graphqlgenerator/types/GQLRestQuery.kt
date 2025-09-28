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
    fun toDocument(allOutputTypes: Collection<GQLOutputType>): String {
        val variables = arguments.entries.joinToString(separator = ", ") { (name, type) -> $$"$$$name: $${type.asReference()}" }
        val fieldArguments = arguments.keys.joinToString { $$"$$it: $$$it" }
        val selectionSet = fieldType.toSelectionSet(allOutputTypes)
        return buildString {
            append("query $operationName($variables) {\n")
            append(indentation()+"$fieldName($fieldArguments)\n")
            append(indentation()+"@rest(\n")
            append(indentation(2)+"type: \"${fieldType.asReference()}\"\n")
            append(indentation(2)+"endpoint: \"spesialist\"\n")
            append(indentation(2)+"path: \"$path\"\n")
            append(indentation(2)+"method: \"GET\"\n")
            append(indentation()+")")
            if (selectionSet.isNotEmpty()) {
                append(" ")
                append(selectionSet.indentNewlines())
            }
            append("\n")
            append("}")
        }
    }

    fun toQueryObjectField(): String =
        "${fieldName}(${arguments.entries.joinToString(separator = ", ") { (name, type) -> "$name: ${type.asReference()}" }}): ${fieldType.asReference()}"
}

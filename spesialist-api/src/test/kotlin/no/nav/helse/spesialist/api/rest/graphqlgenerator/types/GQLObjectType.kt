package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentNewlines
import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentation

class GQLObjectType(
    override val name: String,
    override val implementedInterfaces: List<GQLInterfaceType>,
    override val fields: Map<String, GQLOutputType>
) : GQLNamedOutputType, GQLObjectOrInterfaceType {
    override fun toSDL(): String = buildString {
        append("type ")
        append(name)
        if (implementedInterfaces.isNotEmpty()) {
            append(" implements ")
            append(implementedInterfaces.joinToString(" & ") { it.name })
        }
        append(" {\n")
        append(
            fields.entries.joinToString(
                "\n",
                postfix = "\n"
            ) { (name, type) -> "${indentation()}$name: ${type.asReference()}" })
        append("}")
    }

    override fun toSelectionSet(allOutputTypes: Collection<GQLOutputType>): String =
        toSelectionSet(allOutputTypes = allOutputTypes, alreadySelectedFields = emptySet())

    fun toSelectionSet(
        allOutputTypes: Collection<GQLOutputType>,
        alreadySelectedFields: Set<String>
    ): String = buildString {
        val selections = fields.entries.sortedBy { it.key }
            .filterNot { property -> alreadySelectedFields.contains(property.key) }
            .map { (key, type) ->
                buildString {
                    append(key)
                    val subSelectionSet = type.toSelectionSet(allOutputTypes)
                    if (subSelectionSet.isNotEmpty()) {
                        append(" ")
                        append(subSelectionSet)
                    }
                }
            }
        if (selections.isNotEmpty()) {
            append("{\n")
            selections.forEach { selection ->
                append(indentation())
                append(selection.indentNewlines())
                append("\n")
            }
            append("}")
        }
    }
}

package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentNewlines
import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentation

class GQLInterfaceType(
    override val name: String,
    override val implementedInterfaces: List<GQLInterfaceType>,
    override val fields: Map<String, GQLOutputType>
) : GQLNamedOutputType, GQLObjectOrInterfaceType {
    override fun toSDL(): String = buildString {
        append("interface ")
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

    override fun toSelectionSet(
        allOutputTypes: Collection<GQLOutputType>,
    ): String {
        val fieldSelections = fields.entries.sortedBy { it.key }
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
        val inlineFragments = findImplementors(this, allOutputTypes)
            .sortedBy { it.name }
            .map { it.name to it.toSelectionSet(allOutputTypes = allOutputTypes, alreadySelectedFields = fields.keys) }
            .filterNot { (_, selectionSet) -> selectionSet.isEmpty() }
            .map { (name, selectionSet) -> "... on $name $selectionSet" }
        val selections = fieldSelections + inlineFragments
        return buildString {
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

    private fun findImplementors(
        interfaceType: GQLInterfaceType,
        allOutputTypes: Collection<GQLOutputType>
    ): List<GQLObjectType> =
        allOutputTypes.filterIsInstance<GQLObjectOrInterfaceType>()
            .filter { interfaceType in it.implementedInterfaces }
            .flatMap {
                when (it) {
                    is GQLInterfaceType -> findImplementors(it, allOutputTypes)
                    is GQLObjectType -> listOf(it)
                }
            }
}

package no.nav.helse.spesialist.api.rest.graphqlgenerator

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
        append(fields.entries.joinToString("\n", postfix = "\n") { (name, type) -> "    $name: ${type.asReference()}" })
        append("}\n")
    }

    override fun toSelectionSet(
        indentationLevel: Int,
        allOutputTypes: Collection<GQLOutputType>,
    ): String {
        val fieldSelections = fields.entries.sortedBy { it.key }
            .map { (key, type) -> key + type.toSelectionSet(indentationLevel + 1, allOutputTypes) }
        val inlineFragments = findImplementors(this, allOutputTypes)
            .sortedBy { it.name }
            .map {
                it.name to it.toSelectionSet(
                    indentationLevel = indentationLevel + 1,
                    allOutputTypes = allOutputTypes,
                    alreadySelectedFields = fields.keys
                )
            }
            .filterNot { (_, selectionSet) -> selectionSet.isEmpty() }
            .map { (name, selectionSet) -> "... on $name$selectionSet" }
        val selections = fieldSelections + inlineFragments
        return buildString {
            if (selections.isNotEmpty()) {
                append(" {")
                append("\n")
                selections.forEach { entry ->
                    append(indentation(indentationLevel + 1))
                    append(entry)
                    append("\n")
                }
                append(indentation(indentationLevel))
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

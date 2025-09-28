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
    ): String = buildString {
        val entries =
            fields.entries.sortedBy { it.key }
                .filterNot { property -> implementedInterfaces.any { it.fields.containsKey(property.key) } }
                .map { (key, type) ->
                    key + type.toSelectionSet(indentationLevel + 1, allOutputTypes)
                } +
                    allOutputTypes.filterIsInstance<GQLObjectOrInterfaceType>()
                        .filter { this@GQLInterfaceType in it.implementedInterfaces }
                        .sortedBy { it.name }
                        .map { it.name to it.toSelectionSet(indentationLevel + 1, allOutputTypes) }
                        .filterNot { (_, selectionSet) -> selectionSet.isEmpty() }
                        .map { (name, selectionSet) -> "... on $name$selectionSet" }
        if (entries.isNotEmpty()) {
            append(" {")
            append("\n")
            entries.forEach { entry ->
                append(indentation(indentationLevel + 1))
                append(entry)
                append("\n")
            }
            append(indentation(indentationLevel))
            append("}")
        }
    }
}

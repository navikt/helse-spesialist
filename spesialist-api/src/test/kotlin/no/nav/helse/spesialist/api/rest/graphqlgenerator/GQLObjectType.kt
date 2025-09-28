package no.nav.helse.spesialist.api.rest.graphqlgenerator

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
        append(fields.entries.joinToString("\n", postfix = "\n") { (name, type) -> "    $name: ${type.asReference()}" })
        append("}\n")
    }

    override fun toSelectionSet(
        indentationLevel: Int,
        allOutputTypes: Collection<GQLOutputType>,
    ): String = toSelectionSet(
        indentationLevel = indentationLevel,
        allOutputTypes = allOutputTypes,
        alreadySelectedFields = emptySet()
    )

    fun toSelectionSet(
        indentationLevel: Int,
        allOutputTypes: Collection<GQLOutputType>,
        alreadySelectedFields: Set<String>
    ): String = buildString {
        val selections = fields.entries.sortedBy { it.key }
            .filterNot { property -> alreadySelectedFields.contains(property.key) }
            .map { (key, type) ->
                buildString {
                    append(key)
                    append(type.toSelectionSet(indentationLevel + 1, allOutputTypes))
                }
            }
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

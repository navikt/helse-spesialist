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
    ): String = buildString {
        val entries = fields.entries.sortedBy { it.key }
            .filterNot { property -> implementedInterfaces.any { it.fields.containsKey(property.key) } }
            .map { (key, type) ->
                buildString {
                    append(key)
                    append(type.toSelectionSet(indentationLevel + 1, allOutputTypes))
                }
            }
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

package no.nav.helse.spesialist.api.rest.graphqlgenerator

data class GQLInputObjectType(
    override val name: String,
    val fields: Map<String, GQLTypeReference<GQLInputType>>
) : GQLInputType {
    override fun toSDL(): String = buildString {
        append("input ")
        append(name)
        append(" {\n")
        append(fields.entries.joinToString("\n", postfix = "\n") { (name, type) -> "    $name: $type" })
        append("}\n")
    }
}

package no.nav.helse.spesialist.api.rest.graphqlgenerator

class GQLInputObjectType(
    override val name: String,
    val fields: Map<String, GQLInputType>
) : GQLNamedInputType {
    override fun toSDL(): String = buildString {
        append("input ")
        append(name)
        append(" {\n")
        append(fields.entries.joinToString("\n", postfix = "\n") { (name, type) -> "    $name: ${type.asReference()}" })
        append("}\n")
    }
}

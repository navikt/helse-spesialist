package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentation

class GQLInputObjectType(
    override val name: String,
    val fields: Map<String, GQLInputType>
) : GQLNamedInputType {
    override fun toSDL(): String = buildString {
        append("input ")
        append(name)
        append(" {\n")
        append(
            fields.entries.joinToString(
                "\n",
                postfix = "\n"
            ) { (name, type) -> "${indentation()}$name: ${type.asReference()}" })
        append("}")
    }
}

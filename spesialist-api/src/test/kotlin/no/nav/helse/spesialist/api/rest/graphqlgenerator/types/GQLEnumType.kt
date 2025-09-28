package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

import no.nav.helse.spesialist.api.rest.graphqlgenerator.indentation

class GQLEnumType(
    override val name: String,
    val values: Set<String>
) : GQLNamedInputType, GQLNamedOutputType {
    override fun toSDL(): String =
        "enum $name {\n" +
                values.sorted().joinToString(separator = "\n") { indentation() + it } + "\n" +
                "}"

    override fun toSelectionSet(allOutputTypes: Collection<GQLOutputType>): String = ""
}

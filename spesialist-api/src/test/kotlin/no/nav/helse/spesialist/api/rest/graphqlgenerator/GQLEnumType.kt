package no.nav.helse.spesialist.api.rest.graphqlgenerator

class GQLEnumType(
    override val name: String,
    val values: Set<String>
) : GQLNamedInputType, GQLNamedOutputType {
    override fun toSDL(): String =
        "enum $name {\n" +
                values.sorted().joinToString(separator = "\n") { indentation(1) + it } + "\n" +
                "}"

    override fun toSelectionSet(indentationLevel: Int, allOutputTypes: Collection<GQLOutputType>): String = ""
}

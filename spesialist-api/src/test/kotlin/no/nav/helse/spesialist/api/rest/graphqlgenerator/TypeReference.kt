package no.nav.helse.spesialist.api.rest.graphqlgenerator

class TypeReference(
    val type: TypeDefinition,
    val nullable: Boolean
) {
    override fun toString() = type.name + if (nullable) "" else "!"
}

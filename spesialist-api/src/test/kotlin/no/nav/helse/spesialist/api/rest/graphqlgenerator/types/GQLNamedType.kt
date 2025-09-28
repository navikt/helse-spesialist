package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

sealed interface GQLNamedType: GQLType {
    val name: String
    fun toSDL(): String
    override fun asReference(): String = name
}

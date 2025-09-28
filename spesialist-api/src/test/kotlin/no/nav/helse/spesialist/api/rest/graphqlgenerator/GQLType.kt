package no.nav.helse.spesialist.api.rest.graphqlgenerator

sealed interface GQLType {
    fun asReference(): String
}

sealed interface GQLNamedType: GQLType {
    val name: String
    fun toSDL(): String
    override fun asReference(): String = name
}

package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

sealed interface GQLType {
    fun asReference(): String
}

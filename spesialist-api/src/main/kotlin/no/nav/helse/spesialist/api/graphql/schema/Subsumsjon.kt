package no.nav.helse.spesialist.api.graphql.schema

data class Subsumsjon(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
)
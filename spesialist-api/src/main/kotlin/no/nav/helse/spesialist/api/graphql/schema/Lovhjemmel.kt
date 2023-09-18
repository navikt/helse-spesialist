package no.nav.helse.spesialist.api.graphql.schema

data class Lovhjemmel(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
    val lovverk: String? = null,
    val lovverksversjon: String? = null,
)
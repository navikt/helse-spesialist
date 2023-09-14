package no.nav.helse.db

data class SubsumsjonForDatabase(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
)
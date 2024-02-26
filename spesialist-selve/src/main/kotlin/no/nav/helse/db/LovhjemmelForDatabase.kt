package no.nav.helse.db

data class LovhjemmelForDatabase(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
)
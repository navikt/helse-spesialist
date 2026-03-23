package no.nav.helse.db.overstyring

data class LovhjemmelForDatabase(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
)

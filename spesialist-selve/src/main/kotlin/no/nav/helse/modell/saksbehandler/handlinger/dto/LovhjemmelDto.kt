package no.nav.helse.modell.saksbehandler.handlinger.dto

data class LovhjemmelDto(
    val paragraf: String,
    val ledd: String?,
    val bokstav: String?,
    val lovverk: String?,
    val lovverksversjon: String?,
)
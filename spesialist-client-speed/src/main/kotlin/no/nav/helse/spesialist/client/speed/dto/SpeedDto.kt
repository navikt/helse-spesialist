package no.nav.helse.spesialist.client.speed.dto

internal data class HistoriskeIdenterRequest(
    val ident: String,
)

internal data class HistoriskeIdenterResponse(
    val fødselsnumre: List<String>,
    val kilde: String,
)

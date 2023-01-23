package no.nav.helse.spesialist.api.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties
data class AnnulleringDto(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val saksbehandlerIdent: String,
    val begrunnelser: List<String> = emptyList(),
    val kommentar: String?
)
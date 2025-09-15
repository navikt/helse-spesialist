package no.nav.helse.modell.saksbehandler.handlinger

import java.util.UUID

data class AnnulleringDto(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val årsaker: List<AnnulleringArsak>,
    val kommentar: String?,
)

data class AnnulleringArsak(
    val key: String,
    val arsak: String,
)

package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID

data class Annullering(
    val saksbehandlerIdent: String,
    val arbeidsgiverFagsystemId: String?,
    val personFagsystemId: String?,
    val tidspunkt: LocalDateTime,
    val arsaker: List<String>,
    val begrunnelse: String?,
    val vedtaksperiodeId: UUID?,
)

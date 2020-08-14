package no.nav.helse.mediator.kafka.meldinger

import java.time.LocalDate
import java.util.*

data class BistandSaksbehandlerMessage(
    val id: UUID,
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val orgnummer: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate
)

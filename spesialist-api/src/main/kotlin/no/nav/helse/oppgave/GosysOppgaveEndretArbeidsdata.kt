package no.nav.helse.oppgave

import java.time.LocalDate
import java.util.UUID

data class GosysOppgaveEndretArbeidsdata(
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val utbetalingId: UUID,
    val utbetalingType: String,
    val hendelseId: UUID,
    val godkjenningsbehovJson: String,
)

package no.nav.helse.modell.gosysoppgaver

import java.time.LocalDate
import java.util.UUID

data class GosysOppgaveEndretCommandData(
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val godkjenningsbehovJson: String,
)

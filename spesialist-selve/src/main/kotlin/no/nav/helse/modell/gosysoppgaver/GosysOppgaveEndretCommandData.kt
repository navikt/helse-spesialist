package no.nav.helse.modell.gosysoppgaver

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Periodetype

data class GosysOppgaveEndretCommandData(
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val godkjenningsbehovJson: String,
    val periodetype: Periodetype,
)

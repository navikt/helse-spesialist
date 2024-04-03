package no.nav.helse.modell.gosysoppgaver

import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.Periode.Companion.til
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.time.LocalDate
import java.util.UUID

data class OppgaveDataForAutomatisering(
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val godkjenningsbehovJson: String,
    val periodetype: Periodetype,
) {
    private val periode = periodeFom til periodeTom

    fun periodeOverlapperMed(perioder: List<Periode>): Boolean = perioder.any { periode.overlapperMed(it) }
}

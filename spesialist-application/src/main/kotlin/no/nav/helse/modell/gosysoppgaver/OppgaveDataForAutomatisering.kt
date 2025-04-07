package no.nav.helse.modell.gosysoppgaver

import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import java.time.LocalDate
import java.util.UUID

data class OppgaveDataForAutomatisering(
    val oppgaveId: Long,
    val vedtaksperiodeId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val godkjenningsbehovJson: String,
    val periodetype: Periodetype,
) {
    private val periode = periodeFom tilOgMed periodeTom

    fun periodeOverlapperMed(perioder: List<Periode>): Boolean = perioder.any { periode.overlapper(it) }
}

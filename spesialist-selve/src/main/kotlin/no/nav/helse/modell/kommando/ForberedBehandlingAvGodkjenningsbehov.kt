package no.nav.helse.modell.kommando

import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtaksperiode.SpleisVedtaksperiode
import java.time.LocalDate
import java.util.UUID

internal class ForberedBehandlingAvGodkjenningsbehov(
    private val person: Person,
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val spleisBehandlingId: UUID,
    private val tags: List<String>,
    private val skjæringstidspunkt: LocalDate,
): Command {
    override fun execute(context: CommandContext): Boolean {
        person.mottaSpleisVedtaksperioder(spleisVedtaksperioder)
        person.flyttEventuelleAvviksvarsler(vedtaksperiodeId, skjæringstidspunkt)
        person.oppdaterPeriodeTilGodkjenning(vedtaksperiodeId, tags, spleisBehandlingId, utbetalingId)
        return true
    }
}

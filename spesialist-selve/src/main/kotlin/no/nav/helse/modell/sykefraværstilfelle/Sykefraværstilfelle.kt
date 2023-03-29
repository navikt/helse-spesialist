package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.harAktiveVarsler

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val vedtaksperioder: List<Vedtaksperiode>
) {

    internal fun harAktiveVarsler(tilOgMed: LocalDate, utbetalingId: UUID): Boolean {
        return vedtaksperioder.harAktiveVarsler(tilOgMed, utbetalingId)
    }
}
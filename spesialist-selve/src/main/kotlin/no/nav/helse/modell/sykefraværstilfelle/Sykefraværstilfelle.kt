package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.harAktiveVarsler
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.håndter

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val vedtaksperioder: List<Vedtaksperiode>
) {

    internal fun harAktiveVarsler(tilOgMed: LocalDate): Boolean {
        return vedtaksperioder.harAktiveVarsler(tilOgMed)
    }

    internal fun håndter(varsel: Varsel) {
        vedtaksperioder.håndter(listOf(varsel))
    }

    internal fun deaktiver(varsel: Varsel) {
        vedtaksperioder.deaktiver(varsel)
    }
}
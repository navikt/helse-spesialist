package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.forhindrerAutomatisering
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.håndter

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val vedtaksperioder: List<Vedtaksperiode>
) {

    internal fun forhindrerAutomatisering(tilOgMed: LocalDate): Boolean {
        return vedtaksperioder.forhindrerAutomatisering(tilOgMed)
    }

    internal fun håndter(varsel: Varsel) {
        vedtaksperioder.håndter(listOf(varsel))
    }

    internal fun deaktiver(varsel: Varsel) {
        vedtaksperioder.deaktiver(varsel)
    }
}
package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.forhindrerAutomatisering
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndter

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val gjeldendeGenerasjoner: List<Generasjon>
) {

    internal fun forhindrerAutomatisering(tilOgMed: LocalDate): Boolean {
        return gjeldendeGenerasjoner.forhindrerAutomatisering(tilOgMed)
    }

    internal fun håndter(varsel: Varsel) {
        gjeldendeGenerasjoner.håndter(listOf(varsel))
    }

    internal fun deaktiver(varsel: Varsel) {
        gjeldendeGenerasjoner.deaktiver(varsel)
    }
}
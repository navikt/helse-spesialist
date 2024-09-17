package no.nav.helse.modell.sykefraværstilfelle

import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.forhindrerAutomatisering
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.harKunGosysvarsel
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.harMedlemskapsvarsel
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterGodkjent
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterNyttVarsel
import java.time.LocalDate
import java.util.UUID

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val gjeldendeGenerasjoner: List<Generasjon>,
) {
    init {
        check(gjeldendeGenerasjoner.isNotEmpty()) { "Kan ikke opprette et sykefraværstilfelle uten generasjoner" }
    }

    internal fun skjæringstidspunkt() = skjæringstidspunkt

    internal fun forhindrerAutomatisering(vedtaksperiodeId: UUID): Boolean {
        val generasjonForPeriode =
            gjeldendeGenerasjoner.finnGenerasjonForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde generasjon for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeGenerasjoner.forhindrerAutomatisering(generasjonForPeriode)
    }

    internal fun harKunGosysvarsel(vedtaksperiodeId: UUID): Boolean {
        val generasjonForPeriode =
            gjeldendeGenerasjoner.finnGenerasjonForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde generasjon for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeGenerasjoner.harKunGosysvarsel(generasjonForPeriode)
    }

    internal fun håndter(varsel: Varsel) {
        gjeldendeGenerasjoner.håndterNyttVarsel(listOf(varsel))
    }

    internal fun spesialsakSomKanAutomatiseres(vedtaksperiodeId: UUID): Boolean {
        val generasjon = gjeldendeGenerasjoner.finnGenerasjonForVedtaksperiode(vedtaksperiodeId) ?: return false
        return generasjon.erSpesialsakSomKanAutomatiseres()
    }

    internal fun automatiskGodkjennSpesialsakvarsler(vedtaksperiodeId: UUID) {
        val generasjon =
            gjeldendeGenerasjoner.finnGenerasjonForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Forventer å finne generasjon for perioden")
        generasjon.automatiskGodkjennSpesialsakvarsler()
    }

    internal fun deaktiver(varsel: Varsel) {
        gjeldendeGenerasjoner.deaktiver(varsel)
    }

    internal fun håndterGodkjent(vedtaksperiodeId: UUID) {
        gjeldendeGenerasjoner.håndterGodkjent(vedtaksperiodeId)
    }

    internal fun harMedlemskapsvarsel(vedtaksperiodeId: UUID): Boolean = gjeldendeGenerasjoner.harMedlemskapsvarsel(vedtaksperiodeId)
}

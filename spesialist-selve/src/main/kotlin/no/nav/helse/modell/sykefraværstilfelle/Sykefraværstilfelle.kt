package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastattSykepengegrunnlag.Companion.sortert
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.forhindrerAutomatisering
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterGodkjent
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterNyttVarsel
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.kreverTotrinnsvurdering
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengevedtak
import no.nav.helse.modell.vedtaksperiode.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.vedtak.UtkastTilVedtak

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val gjeldendeGenerasjoner: List<Generasjon>,
    skjønnsfastatteSykepengegrunnlag: List<SkjønnsfastattSykepengegrunnlag>,
) {
    private val skjønnsfastatteSykepengegrunnlag = skjønnsfastatteSykepengegrunnlag.sortert()
    private val observers = mutableListOf<SykefraværstilfelleObserver>()

    private fun fattVedtak(vedtak: Sykepengevedtak) = observers.forEach { it.vedtakFattet(vedtak) }

    private fun deaktiverVarsel(varsel: Varsel) = observers.forEach { it.deaktiverVarsel(varsel) }

    internal fun haster(vedtaksperiodeId: UUID): Boolean {
        val generasjon = gjeldendeGenerasjoner.finnGenerasjon(vedtaksperiodeId)
            ?: throw IllegalArgumentException("Finner ikke generasjon med vedtaksperiodeId=$vedtaksperiodeId i sykefraværstilfelle med skjæringstidspunkt=$skjæringstidspunkt")
        return generasjon.hasterÅBehandle()
    }

    internal fun forhindrerAutomatisering(tilOgMed: LocalDate): Boolean {
        return gjeldendeGenerasjoner.forhindrerAutomatisering(tilOgMed)
    }

    internal fun håndter(varsel: Varsel, hendelseId: UUID) {
        gjeldendeGenerasjoner.håndterNyttVarsel(listOf(varsel), hendelseId)
    }

    internal fun håndter(utkastTilVedtak: UtkastTilVedtak) {
        val vedtakBuilder = SykepengevedtakBuilder()
        val skjønnsfastsattSykepengegrunnlag = skjønnsfastatteSykepengegrunnlag.lastOrNull()
        skjønnsfastsattSykepengegrunnlag?.also {
            vedtakBuilder.skjønnsfastsattSykepengegrunnlag(it)
        }
        utkastTilVedtak.byggVedtak(vedtakBuilder)
        fattVedtak(vedtakBuilder.build())
    }

    internal fun deaktiver(varsel: Varsel) {
        gjeldendeGenerasjoner.deaktiver(varsel)
        deaktiverVarsel(varsel)
    }

    internal fun håndterGodkjent(saksbehandlerIdent: String, vedtaksperiodeId: UUID, hendelseId: UUID) {
        gjeldendeGenerasjoner.håndterGodkjent(saksbehandlerIdent, vedtaksperiodeId, hendelseId)
    }

    internal fun kreverTotrinnsvurdering(vedtaksperiodeId: UUID): Boolean {
        return gjeldendeGenerasjoner.kreverTotrinnsvurdering(vedtaksperiodeId)
    }

    internal fun registrer(observer: SykefraværstilfelleObserver) {
        observers.add(observer)
    }
}
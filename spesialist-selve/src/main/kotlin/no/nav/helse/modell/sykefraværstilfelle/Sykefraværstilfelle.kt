package no.nav.helse.modell.sykefraværstilfelle

import no.nav.helse.modell.person.PersonObserver
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag.Companion.sortert
import no.nav.helse.modell.vedtak.Sykepengevedtak
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.erTilbakedatert
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.forhindrerAutomatisering
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterGodkjent
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterNyttVarsel
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.kreverSkjønnsfastsettelse
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.kreverTotrinnsvurdering
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetMedVedtak
import java.time.LocalDate
import java.util.UUID

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val gjeldendeGenerasjoner: List<Generasjon>,
    skjønnsfastatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlag>,
) {
    init {
        check(gjeldendeGenerasjoner.isNotEmpty()) { "Kan ikke opprette et sykefraværstilfelle uten generasjoner" }
    }

    private val skjønnsfastatteSykepengegrunnlag = skjønnsfastatteSykepengegrunnlag.sortert()
    private val observers = mutableListOf<PersonObserver>()

    private fun fattVedtak(vedtak: Sykepengevedtak) = observers.forEach { it.vedtakFattet(vedtak) }

    internal fun haster(vedtaksperiodeId: UUID): Boolean {
        val generasjon =
            gjeldendeGenerasjoner.finnGenerasjonForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalArgumentException(
                    "Finner ikke generasjon med vedtaksperiodeId=$vedtaksperiodeId i sykefraværstilfelle med skjæringstidspunkt=$skjæringstidspunkt",
                )
        return generasjon.hasterÅBehandle()
    }

    internal fun forhindrerAutomatisering(vedtaksperiodeId: UUID): Boolean {
        val generasjonForPeriode =
            gjeldendeGenerasjoner.finnGenerasjonForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde generasjon for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeGenerasjoner.forhindrerAutomatisering(generasjonForPeriode)
    }

    internal fun håndter(
        varsel: Varsel,
        hendelseId: UUID,
    ) {
        gjeldendeGenerasjoner.håndterNyttVarsel(listOf(varsel), hendelseId)
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

    internal fun håndter(
        avsluttetMedVedtak: AvsluttetMedVedtak,
        tags: List<String>,
    ) {
        val vedtakBuilder = SykepengevedtakBuilder()
        val skjønnsfastsattSykepengegrunnlag = skjønnsfastatteSykepengegrunnlag.lastOrNull()
        skjønnsfastsattSykepengegrunnlag?.also {
            vedtakBuilder.skjønnsfastsattSykepengegrunnlag(it)
        }
        if (tags.isNotEmpty()) {
            vedtakBuilder.tags(tags)
        }
        avsluttetMedVedtak.byggVedtak(vedtakBuilder)
        fattVedtak(vedtakBuilder.build())
    }

    internal fun deaktiver(varsel: Varsel) {
        gjeldendeGenerasjoner.deaktiver(varsel)
    }

    internal fun håndterGodkjent(
        saksbehandlerIdent: String,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        gjeldendeGenerasjoner.håndterGodkjent(saksbehandlerIdent, vedtaksperiodeId, hendelseId)
    }

    internal fun kreverTotrinnsvurdering(vedtaksperiodeId: UUID): Boolean {
        return gjeldendeGenerasjoner.kreverTotrinnsvurdering(vedtaksperiodeId)
    }

    internal fun kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean {
        return gjeldendeGenerasjoner.kreverSkjønnsfastsettelse(vedtaksperiodeId)
    }

    internal fun erTilbakedatert(vedtaksperiodeId: UUID): Boolean {
        return gjeldendeGenerasjoner.erTilbakedatert(vedtaksperiodeId)
    }

    internal fun registrer(observer: PersonObserver) {
        observers.add(observer)
    }
}

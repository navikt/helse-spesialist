package no.nav.helse.modell.person

import no.nav.helse.modell.hendelse.Sykepengevedtak
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.flyttEventueltAvviksvarselTil
import no.nav.helse.modell.person.vedtaksperiode.Periode
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.finnBehandling
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.relevanteFor
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.AvsluttetMedVedtak
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag.Companion.relevanteFor
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    vedtaksperioder: List<Vedtaksperiode>,
    private val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlag>,
    private val avviksvurderinger: List<Avviksvurdering>,
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()
    private val observers = mutableSetOf<PersonObserver>()

    fun nyObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    fun toDto() =
        PersonDto(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = vedtaksperioder.map { it.toDto() },
            avviksvurderinger = avviksvurderinger.map { it.toDto() },
            skjønnsfastsatteSykepengegrunnlag = skjønnsfastsatteSykepengegrunnlag.map { it.toDto() },
        )

    fun flyttEventuelleAvviksvarsler(
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
    ) {
        vedtaksperioder.relevanteFor(skjæringstidspunkt).flyttEventueltAvviksvarselTil(vedtaksperiodeId)
    }

    fun behandleTilbakedateringBehandlet(perioder: List<Periode>) {
        vedtaksperioder.forEach { it.behandleTilbakedateringGodkjent(perioder) }
    }

    fun mottaSpleisVedtaksperioder(perioder: List<SpleisVedtaksperiode>) {
        vedtaksperioder.forEach { it.nyttGodkjenningsbehov(perioder) }
    }

    fun oppdaterPeriodeTilGodkjenning(
        vedtaksperiodeId: UUID,
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        vedtaksperiodeOrNull(vedtaksperiodeId)?.mottaBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)
    }

    fun vedtakFattet(
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
    ) {
        vedtaksperiodeOrNull(vedtaksperiodeId)
            ?.vedtakFattet(spleisBehandlingId)
    }

    fun fattVedtak(avsluttetMedVedtak: AvsluttetMedVedtak) {
        val vedtakBuilder = SykepengevedtakBuilder()
        val vedtaksperiode = vedtaksperiode(avsluttetMedVedtak.spleisBehandlingId)
        val behandling = vedtaksperiode.finnBehandling(avsluttetMedVedtak.spleisBehandlingId)

        avviksvurderinger.finnRiktigAvviksvurdering(behandling.skjæringstidspunkt())?.also {
            vedtakBuilder.sammenligningsgrunnlag(it.sammenligningsgrunnlag)
            vedtakBuilder.avviksprosent(it.avviksprosent)
        }

        val skjønnsfastsattSykepengegrunnlag =
            skjønnsfastsatteSykepengegrunnlag
                .relevanteFor(behandling.skjæringstidspunkt())
                .lastOrNull()

        skjønnsfastsattSykepengegrunnlag?.also {
            vedtakBuilder.skjønnsfastsattSykepengegrunnlag(it)
        }
        vedtaksperiode.byggVedtak(vedtakBuilder)
        behandling.byggVedtak(vedtakBuilder)
        avsluttetMedVedtak.byggVedtak(vedtakBuilder)
        byggVedtak(vedtakBuilder)

        fattVedtak(vedtakBuilder.build())
    }

    fun avsluttetUtenVedtak(avsluttetUtenVedtak: AvsluttetUtenVedtak) {
        vedtaksperiodeOrNull(avsluttetUtenVedtak.vedtaksperiodeId())
            ?.avsluttetUtenVedtak(this, avsluttetUtenVedtak)
    }

    fun vedtaksperiodeForkastet(vedtaksperiodeId: UUID) {
        vedtaksperioder.find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?.vedtaksperiodeForkastet()
    }

    internal fun supplerVedtakFattet(sykepengevedtakBuilder: SykepengevedtakBuilder) {
        sykepengevedtakBuilder
            .aktørId(aktørId)
            .fødselsnummer(fødselsnummer)
        observers.forEach { it.sykepengevedtak(sykepengevedtakBuilder.build()) }
    }

    fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        vedtaksperioder
            .find { spleisBehandling.erRelevantFor(it.vedtaksperiodeId()) }
            ?.nySpleisBehandling(spleisBehandling)
            ?: vedtaksperioder.add(Vedtaksperiode.nyVedtaksperiode(spleisBehandling))
    }

    fun vedtaksperiodeOrNull(vedtaksperiodeId: UUID): Vedtaksperiode? {
        return vedtaksperioder.find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?: logg.warn("Vedtaksperiode med id={} finnes ikke", vedtaksperiodeId).let { return null }
    }

    private fun vedtaksperiode(spleisBehandlingId: UUID): Vedtaksperiode {
        return vedtaksperioder.finnBehandling(spleisBehandlingId)
            ?: throw IllegalStateException("Behandling med spleisBehandlingId=$spleisBehandlingId finnes ikke")
    }

    fun sykefraværstilfelle(vedtaksperiodeId: UUID): Sykefraværstilfelle {
        val skjæringstidspunkt =
            vedtaksperiodeOrNull(vedtaksperiodeId)?.gjeldendeSkjæringstidspunkt
                ?: throw IllegalStateException("Forventer å finne vedtaksperiode med id=$vedtaksperiodeId")
        val gjeldendeBehandlinger = vedtaksperioder.relevanteFor(skjæringstidspunkt)
        return Sykefraværstilfelle(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            gjeldendeBehandlinger = gjeldendeBehandlinger,
        )
    }

    fun nyeVarsler(varsler: List<Varsel>) {
        vedtaksperioder.forEach { it.nyeVarsler(varsler) }
    }

    fun utbetalingForkastet(utbetalingId: UUID) {
        vedtaksperioder.forEach {
            it.utbetalingForkastet(utbetalingId)
        }
    }

    fun nyUtbetalingForVedtaksperiode(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        vedtaksperiodeOrNull(vedtaksperiodeId)
            ?.nyUtbetaling(utbetalingId)
    }

    private fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.fødselsnummer(fødselsnummer)
        vedtakBuilder.aktørId(aktørId)
    }

    private fun fattVedtak(vedtak: Sykepengevedtak) = observers.forEach { it.sykepengevedtak(vedtak) }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

        fun gjenopprett(
            aktørId: String,
            fødselsnummer: String,
            vedtaksperioder: List<VedtaksperiodeDto>,
            skjønnsfastsattSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
            avviksvurderinger: List<AvviksvurderingDto>,
        ): Person {
            return Person(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vedtaksperioder =
                    vedtaksperioder.map {
                        Vedtaksperiode.gjenopprett(
                            organisasjonsnummer = it.organisasjonsnummer,
                            vedtaksperiodeId = it.vedtaksperiodeId,
                            forkastet = it.forkastet,
                            behandlinger = it.behandlinger,
                        )
                    },
                avviksvurderinger = avviksvurderinger.map { Avviksvurdering.gjenopprett(it) },
                skjønnsfastsatteSykepengegrunnlag =
                    skjønnsfastsattSykepengegrunnlag
                        .sortedBy { it.opprettet }
                        .map {
                            SkjønnsfastsattSykepengegrunnlag.gjenopprett(
                                type = it.type,
                                årsak = it.årsak,
                                skjæringstidspunkt = it.skjæringstidspunkt,
                                begrunnelseFraMal = it.begrunnelseFraMal,
                                begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                                begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                                opprettet = it.opprettet,
                            )
                        },
            )
        }
    }
}

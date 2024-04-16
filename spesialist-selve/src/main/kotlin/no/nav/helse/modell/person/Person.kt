package no.nav.helse.modell.person

import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtaksperiode.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import org.slf4j.LoggerFactory
import java.util.UUID

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    vedtaksperioder: List<Vedtaksperiode>,
    private val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlag>,
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()
    private val observers = mutableSetOf<PersonObserver>()

    internal fun nyObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    private fun finnVedtaksperiode(vedtaksperiodeId: UUID): Vedtaksperiode? {
        return vedtaksperioder.find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?: logg.warn("Vedtaksperiode med id={} finnes ikke", vedtaksperiodeId).let { return null }
    }

    fun toDto() =
        PersonDto(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = vedtaksperioder.map { it.toDto() },
            skjønnsfastsatteSykepengegrunnlag = skjønnsfastsatteSykepengegrunnlag.map { it.toDto() },
        )

    fun behandleTilbakedateringBehandlet(perioder: List<Periode>) {
        vedtaksperioder.forEach { it.behandleTilbakedateringGodkjent(perioder) }
    }

    fun mottaSpleisVedtaksperioder(perioder: List<SpleisVedtaksperiode>) {
        vedtaksperioder.forEach { it.nyttGodkjenningsbehov(perioder) }
    }

    internal fun vedtakFattet(vedtakFattet: VedtakFattet) {
        finnVedtaksperiode(vedtakFattet.vedtaksperiodeId())
            ?.vedtakFattet(vedtakFattet.id, vedtakFattet.spleisBehandlingId())
    }

    internal fun avsluttetUtenVedtak(avsluttetUtenVedtak: AvsluttetUtenVedtak) {
        finnVedtaksperiode(avsluttetUtenVedtak.vedtaksperiodeId())
            ?.avsluttetUtenVedtak(this, avsluttetUtenVedtak)
    }

    internal fun vedtaksperiodeForkastet(vedtaksperiodeForkastet: VedtaksperiodeForkastet) {
        finnVedtaksperiode(vedtaksperiodeForkastet.vedtaksperiodeId())
            ?.vedtaksperiodeForkastet()
    }

    internal fun supplerVedtakFattet(sykepengevedtakBuilder: SykepengevedtakBuilder) {
        sykepengevedtakBuilder
            .aktørId(aktørId)
            .fødselsnummer(fødselsnummer)
        observers.forEach { it.vedtakFattet(sykepengevedtakBuilder.build()) }
    }

    fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        vedtaksperioder
            .find { spleisBehandling.erRelevantFor(it.vedtaksperiodeId()) }
            ?.nySpleisBehandling(spleisBehandling)
            ?: vedtaksperioder.add(Vedtaksperiode.nyVedtaksperiode(spleisBehandling))
    }

    internal fun nyeVarsler(nyeVarsler: NyeVarsler) {
        vedtaksperioder.forEach { it.nyeVarsler(nyeVarsler.varsler) }
    }

    internal fun utbetalingForkastet(utbetalingEndret: UtbetalingEndret) {
        vedtaksperioder.forEach {
            it.utbetalingForkastet(utbetalingEndret)
        }
    }

    internal fun nyUtbetalingForVedtaksperiode(vedtaksperiodeNyUtbetaling: VedtaksperiodeNyUtbetaling) {
        finnVedtaksperiode(vedtaksperiodeNyUtbetaling.vedtaksperiodeId())
            ?.nyUtbetaling(vedtaksperiodeNyUtbetaling.id, vedtaksperiodeNyUtbetaling.utbetalingId)
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

        fun gjenopprett(
            aktørId: String,
            fødselsnummer: String,
            vedtaksperioder: List<VedtaksperiodeDto>,
            skjønnsfastsattSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
        ): Person {
            return Person(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vedtaksperioder =
                    vedtaksperioder.map {
                        Vedtaksperiode.gjenopprett(
                            it.organisasjonsnummer,
                            it.vedtaksperiodeId,
                            it.forkastet,
                            it.generasjoner,
                        )
                    },
                skjønnsfastsatteSykepengegrunnlag =
                    skjønnsfastsattSykepengegrunnlag.map {
                        SkjønnsfastsattSykepengegrunnlag.gjenopprett(
                            it.type,
                            it.årsak,
                            it.skjæringstidspunkt,
                            it.begrunnelseFraMal,
                            it.begrunnelseFraFritekst,
                            it.begrunnelseFraKonklusjon,
                            it.opprettet,
                        )
                    },
            )
        }
    }
}

package no.nav.helse.modell.person

import java.util.UUID
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

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    vedtaksperioder: List<Vedtaksperiode>
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()
    private val observers = mutableSetOf<PersonObserver>()

    internal fun nyObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    private fun finnVedtaksperiode(vedtaksperiodeId: UUID): Vedtaksperiode {
        return vedtaksperioder.find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?: throw IllegalStateException("Forventer at vedtaksperiode med id=$vedtaksperiodeId finnes")
    }

    fun toDto() = PersonDto(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        vedtaksperioder = vedtaksperioder.map { it.toDto() }
    )

    fun behandleTilbakedateringBehandlet(perioder: List<Periode>) {
        vedtaksperioder.forEach { it.behandleTilbakedateringGodkjent(perioder) }
    }

    fun mottaSpleisVedtaksperioder(perioder: List<SpleisVedtaksperiode>) {
        vedtaksperioder.forEach { it.håndter(perioder) }
    }

    internal fun vedtakFattet(vedtakFattet: VedtakFattet) {
        finnVedtaksperiode(vedtakFattet.vedtaksperiodeId())
            .vedtakFattet(vedtakFattet.id)
    }

    internal fun avsluttetUtenVedtak(avsluttetUtenVedtak: AvsluttetUtenVedtak) {
        finnVedtaksperiode(avsluttetUtenVedtak.vedtaksperiodeId())
            .avsluttetUtenVedtak(this, avsluttetUtenVedtak)
    }

    internal fun vedtaksperiodeForkastet(vedtaksperiodeForkastet: VedtaksperiodeForkastet ) {
        finnVedtaksperiode(vedtaksperiodeForkastet.vedtaksperiodeId())
            .vedtaksperiodeForkastet()
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
            .nyUtbetaling(vedtaksperiodeNyUtbetaling.id, vedtaksperiodeNyUtbetaling.utbetalingId)
    }

    companion object {
        fun gjenopprett(aktørId: String, fødselsnummer: String, vedtaksperioder: List<VedtaksperiodeDto>): Person {
            return Person(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vedtaksperioder = vedtaksperioder.map {
                    Vedtaksperiode.gjenopprett(it.organisasjonsnummer, it.vedtaksperiodeId, it.forkastet, it.generasjoner)
                }
            )
        }
    }
}
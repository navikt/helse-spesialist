package no.nav.helse.modell.person

import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    vedtaksperioder: List<Vedtaksperiode>
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()

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
        vedtaksperioder
            .find { vedtakFattet.erRelevantFor(it.vedtaksperiodeId()) }
            ?.vedtakFattet(vedtakFattet.id)
    }
    internal fun vedtaksperiodeForkastet(vedtaksperiodeForkastet: VedtaksperiodeForkastet ) {
        vedtaksperioder
            .find { vedtaksperiodeForkastet.erRelevantFor(it.vedtaksperiodeId()) }
            ?.vedtaksperiodeForkastet()
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
        vedtaksperioder
            .find { vedtaksperiodeNyUtbetaling.erRelevantFor(it.vedtaksperiodeId()) }
            ?.nyUtbetaling(vedtaksperiodeNyUtbetaling.id, vedtaksperiodeNyUtbetaling.utbetalingId)
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
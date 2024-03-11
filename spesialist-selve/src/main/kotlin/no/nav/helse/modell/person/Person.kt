package no.nav.helse.modell.person

import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto

class Person private constructor(
    private val fødselsnummer: String,
    vedtaksperioder: List<Vedtaksperiode>
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()

    fun toDto() = PersonDto(
        fødselsnummer = fødselsnummer,
        vedtaksperioder = vedtaksperioder.map { it.toDto() }
    )

    fun behandleTilbakedateringBehandlet(perioder: List<Periode>) {
        vedtaksperioder.forEach { it.behandleTilbakedateringGodkjent(perioder) }
    }

    fun mottaSpleisVedtaksperioder(perioder: List<SpleisVedtaksperiode>) {
        vedtaksperioder.forEach { it.håndter(perioder) }
    }

    fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        vedtaksperioder
            .find { spleisBehandling.erRelevantFor(it.vedtaksperiodeId()) }
            ?.nySpleisBehandling(spleisBehandling)
            ?: vedtaksperioder.add(Vedtaksperiode.nyVedtaksperiode(spleisBehandling))
    }

    companion object {
        fun gjenopprett(fødselsnummer: String, vedtaksperioder: List<VedtaksperiodeDto>): Person {
            return Person(
                fødselsnummer,
                vedtaksperioder.map { Vedtaksperiode.gjenopprett(it.organisasjonsnummer, it.vedtaksperiodeId, it.generasjoner) }
            )
        }
    }
}
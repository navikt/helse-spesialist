package no.nav.helse.modell.person

import no.nav.helse.modell.vedtaksperiode.Periode
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

    companion object {
        fun gjenopprett(fødselsnummer: String, vedtaksperioder: List<VedtaksperiodeDto>): Person {
            return Person(
                fødselsnummer,
                vedtaksperioder.map { Vedtaksperiode.gjenopprett(it.vedtaksperiodeId, it.generasjoner) }
            )
        }
    }
}
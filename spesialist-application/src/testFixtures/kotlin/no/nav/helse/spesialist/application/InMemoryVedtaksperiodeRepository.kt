package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto

class InMemoryVedtaksperiodeRepository: VedtaksperiodeRepository {
    val vedtaksperioderPerFødselsnummer = mutableMapOf<String, MutableList<VedtaksperiodeDto>>()

    override fun finnVedtaksperioder(fødselsnummer: String) =
        vedtaksperioderPerFødselsnummer[fødselsnummer] ?: emptyList()

    override fun lagreVedtaksperioder(
        fødselsnummer: String,
        vedtaksperioder: List<VedtaksperiodeDto>
    ) {
        val list = vedtaksperioderPerFødselsnummer.getOrPut(fødselsnummer) { mutableListOf() }
        list.removeAll { it.vedtaksperiodeId in vedtaksperioder.map(VedtaksperiodeDto::vedtaksperiodeId) }
        list.addAll(vedtaksperioder)
    }

    override fun førsteKjenteDag(fødselsnummer: String) =
        finnVedtaksperioder(fødselsnummer).flatMap { it.behandlinger }.minOfOrNull { it.fom }
}

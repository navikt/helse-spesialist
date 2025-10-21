package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import java.util.UUID

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

    fun alle(): List<VedtaksperiodeDto> = vedtaksperioderPerFødselsnummer.values.flatten()

    fun finnFødselsnummer(vedtaksperiodeId: UUID): String =
        vedtaksperioderPerFødselsnummer.entries.first { (_, vedtaksperioder) -> vedtaksperioder.any { it.vedtaksperiodeId == vedtaksperiodeId } }.key

    override fun førsteKjenteDag(fødselsnummer: String) =
        finnVedtaksperioder(fødselsnummer).flatMap { it.behandlinger }.minOfOrNull { it.fom }
}

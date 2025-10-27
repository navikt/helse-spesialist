package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class InMemoryVedtaksperiodeRepository: VedtaksperiodeRepository {
    private val vedtaksperioder = mutableMapOf<VedtaksperiodeId, Vedtaksperiode>()
    override fun finn(vedtaksperiodeId: VedtaksperiodeId): Vedtaksperiode? {
        return vedtaksperioder[vedtaksperiodeId]
    }

    fun lagre(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder[vedtaksperiode.id()] = vedtaksperiode
    }
}

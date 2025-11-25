package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.SpleisBehandlingId

class InMemoryMidlertidigBehandlingVedtakFattetDao: MidlertidigBehandlingVedtakFattetDao {
    private val vedtakFattet = mutableSetOf<SpleisBehandlingId>()
    override fun vedtakFattet(spleisBehandlingId: SpleisBehandlingId) {
        vedtakFattet.add(spleisBehandlingId)
    }

    override fun erVedtakFattet(spleisBehandlingId: SpleisBehandlingId): Boolean {
        return vedtakFattet.contains(spleisBehandlingId)
    }
}

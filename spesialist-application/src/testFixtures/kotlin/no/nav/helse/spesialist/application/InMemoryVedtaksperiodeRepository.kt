package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.util.UUID

class InMemoryVedtaksperiodeRepository : VedtaksperiodeRepository,
    AbstractInMemoryRepository<VedtaksperiodeId, Vedtaksperiode>() {
    override fun generateId(): VedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
}

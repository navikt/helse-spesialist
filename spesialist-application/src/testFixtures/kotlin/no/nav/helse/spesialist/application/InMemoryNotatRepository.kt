package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import java.util.UUID

class InMemoryNotatRepository : NotatRepository, AbstractInMemoryRepository<NotatId, Notat>() {
    override fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat> =
        alle().filter { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun generateId(): NotatId = NotatId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)
}

package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.PåVentId

class InMemoryPåVentRepository : PåVentRepository, AbstractInMemoryRepository<PåVentId, PåVent>() {
    override fun generateId(): PåVentId = PåVentId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)
}

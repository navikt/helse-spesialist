package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

class InMemorySaksbehandlerRepository : SaksbehandlerRepository,
    AbstractInMemoryRepository<SaksbehandlerOid, Saksbehandler>() {
    override fun generateId(): SaksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
    override fun deepCopy(original: Saksbehandler): Saksbehandler = Saksbehandler(
        id = original.id(),
        navn = original.navn,
        epost = original.epost,
        ident = original.ident,
    )
}

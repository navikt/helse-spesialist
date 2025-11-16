package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid

class InMemorySaksbehandlerRepository : SaksbehandlerRepository,
    AbstractInMemoryRepository<SaksbehandlerOid, Saksbehandler>() {
    override fun tildelIder(root: Saksbehandler) {
        // ID er satt på forhånd, trenger aldri tildele en fra databasen
    }

    override fun deepCopy(original: Saksbehandler): Saksbehandler = Saksbehandler(
        id = original.id(),
        navn = original.navn,
        epost = original.epost,
        ident = original.ident,
    )
}

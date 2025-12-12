package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid

class InMemorySaksbehandlerRepository :
    AbstractInMemoryRepository<SaksbehandlerOid, Saksbehandler>(),
    SaksbehandlerRepository {
    override fun deepCopy(original: Saksbehandler): Saksbehandler =
        Saksbehandler(
            id = original.id,
            navn = original.navn,
            epost = original.epost,
            ident = original.ident,
        )

    override fun finn(ident: String): Saksbehandler? = alle().find { it.ident == ident }
}

package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid

class InMemorySaksbehandlerRepository : SaksbehandlerRepository {
    val data = mutableMapOf<SaksbehandlerOid, Saksbehandler>()

    override fun lagre(saksbehandler: Saksbehandler) {
        data[saksbehandler.id()] = saksbehandler
    }

    override fun finn(oid: SaksbehandlerOid): Saksbehandler? =
        data[oid]
}

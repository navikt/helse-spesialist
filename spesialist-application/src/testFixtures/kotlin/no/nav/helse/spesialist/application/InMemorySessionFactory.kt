package no.nav.helse.spesialist.application

import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory

class InMemorySessionFactory(val sessionContext: InMemorySessionContext) : SessionFactory {
    override fun <T> transactionalSessionScope(transactionalBlock: (SessionContext) -> T) =
        transactionalBlock(sessionContext)
}

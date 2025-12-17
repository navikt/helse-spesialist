package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos

fun testMedSessionContext(block: (SessionContext) -> Unit) {
    InMemoryRepositoriesAndDaos().sessionFactory.transactionalSessionScope {
        block(it)
    }
}

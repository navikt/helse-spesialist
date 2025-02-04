package no.nav.helse.db

interface SessionFactory {
    fun <T> transactionalSessionScope(transactionalBlock: (SessionContext) -> T): T
}

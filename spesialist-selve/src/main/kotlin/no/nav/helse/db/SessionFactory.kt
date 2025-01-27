package no.nav.helse.db

interface SessionFactory {
    fun transactionalSessionScope(transactionalBlock: (SessionContext) -> Unit)
}

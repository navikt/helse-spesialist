package no.nav.helse.db

interface SessionFactory {
    fun sessionScope(transactionalBlock: (SessionContext) -> Unit)
}

package no.nav.helse.spesialist.db

import no.nav.helse.db.ListenerFactory
import no.nav.helse.spesialist.application.OpptegnelseListener
import java.sql.Connection

typealias ConnectionProvider = () -> Connection

class PgListenerFactory(
    private val connectionProvider: ConnectionProvider,
) : ListenerFactory {
    override suspend fun opptegnelseListener(block: suspend OpptegnelseListener.() -> Unit) {
        PgOpptegnelseListener(connectionProvider).use {
            it.block()
        }
    }
}

package no.nav.helse.spesialist.db

import no.nav.helse.db.ListenerFactory
import no.nav.helse.spesialist.application.OpptegnelseListener
import java.sql.Connection

class PgListenerFactory(
    private val connection: Connection,
) : ListenerFactory {
    override fun opptegnelseListener(): OpptegnelseListener = PgOpptegnelseListener(connection)
}

package no.nav.helse.spesialist.db

import kotliquery.sessionOf
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import javax.sql.DataSource

class TransactionalSessionFactory(
    private val dataSource: DataSource,
) : SessionFactory {
    override fun <T> transactionalSessionScope(transactionalBlock: (SessionContext) -> T): T =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transactionalSession ->
                transactionalBlock(DBSessionContext(transactionalSession))
            }
        }
}

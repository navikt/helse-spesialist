package no.nav.helse.db

import kotliquery.sessionOf
import javax.sql.DataSource

class TransactionalSessionFactory(
    private val dataSource: DataSource,
) : SessionFactory {
    override fun transactionalSessionScope(transactionalBlock: (SessionContext) -> Unit) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transactionalSession ->
                transactionalBlock(DBSessionContext(transactionalSession))
            }
        }
    }
}

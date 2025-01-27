package no.nav.helse.db

import kotliquery.sessionOf
import javax.sql.DataSource

class TransactionalSessionFactory(
    private val dataSource: DataSource,
    private val repositories: Repositories,
) : SessionFactory {
    override fun sessionScope(transactionalBlock: (SessionContext) -> Unit) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transactionalSession ->
                transactionalBlock(repositories.withSessionContext(transactionalSession))
            }
        }
    }
}

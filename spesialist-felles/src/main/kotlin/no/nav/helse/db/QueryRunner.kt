package no.nav.helse.db

import io.ktor.utils.io.core.use
import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.action.QueryAction
import kotliquery.sessionOf
import javax.sql.DataSource

interface QueryRunner {
    fun <T> Query.single(mapping: (Row) -> T?): T?

    fun <T> Query.list(mapping: (Row) -> T?): List<T>

    fun Query.update(): Int

    fun Query.updateAndReturnGeneratedKey(): Long?
}

class MedSession(private val session: Session) : QueryRunner {
    override fun <T> Query.single(mapping: (Row) -> T?) = session.run(map(mapping).asSingle)

    override fun <T> Query.list(mapping: (Row) -> T?) = session.run(map(mapping).asList)

    override fun Query.update() = session.run(asUpdate)

    override fun Query.updateAndReturnGeneratedKey() = session.run(asUpdateAndReturnGeneratedKey)
}

class MedDataSource(private val dataSource: DataSource) : QueryRunner {
    private fun <T> QueryAction<T>.runInSession() = sessionOf(dataSource).use(::runWithSession)

    override fun <T> Query.single(mapping: (Row) -> T?) = map(mapping).asSingle.runInSession()

    override fun <T> Query.list(mapping: (Row) -> T?) = map(mapping).asList.runInSession()

    override fun Query.update() = asUpdate.runInSession()

    override fun Query.updateAndReturnGeneratedKey() = asUpdateAndReturnGeneratedKey.runInSession()
}

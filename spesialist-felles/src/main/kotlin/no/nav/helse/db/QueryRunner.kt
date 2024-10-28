package no.nav.helse.db

import io.ktor.utils.io.core.use
import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.action.QueryAction
import kotliquery.sessionOf
import javax.sql.DataSource

interface QueryRunner {
    fun <T> Query.singleOrNull(mapping: (Row) -> T?): T?

    fun <T> Query.single(mapping: (Row) -> T): T

    fun <T> Query.list(mapping: (Row) -> T?): List<T>

    fun Query.update(): Int

    fun Query.updateAndReturnGeneratedKeyOrNull(): Long?

    fun Query.updateAndReturnGeneratedKey(): Long
}

class MedSession(private val session: Session) : QueryRunner {
    override fun <T> Query.singleOrNull(mapping: (Row) -> T?) = session.run(map(mapping).asSingle)

    override fun <T> Query.single(mapping: (Row) -> T): T =
        requireNotNull(session.run(map(mapping).asSingle)) {
            "Forventer at ${this.statement} ikke gir null"
        }

    override fun <T> Query.list(mapping: (Row) -> T?) = session.run(map(mapping).asList)

    override fun Query.update() = session.run(asUpdate)

    override fun Query.updateAndReturnGeneratedKeyOrNull() = session.run(asUpdateAndReturnGeneratedKey)

    override fun Query.updateAndReturnGeneratedKey() =
        requireNotNull(session.run(asUpdateAndReturnGeneratedKey)) {
            "Forventer at ${this.statement} ikke gir null"
        }
}

class MedDataSource(private val dataSource: DataSource) : QueryRunner {
    private fun <T> QueryAction<T>.runInSession(returnGeneratedKey: Boolean = false) =
        sessionOf(dataSource, returnGeneratedKey).use(::runWithSession)

    override fun <T> Query.single(mapping: (Row) -> T): T =
        requireNotNull(map(mapping).asSingle.runInSession()) {
            "Forventer at ${this.statement} ikke gir null"
        }

    override fun <T> Query.singleOrNull(mapping: (Row) -> T?) = map(mapping).asSingle.runInSession()

    override fun <T> Query.list(mapping: (Row) -> T?) = map(mapping).asList.runInSession()

    override fun Query.update() = asUpdate.runInSession()

    override fun Query.updateAndReturnGeneratedKeyOrNull() = asUpdateAndReturnGeneratedKey.runInSession(returnGeneratedKey = true)

    override fun Query.updateAndReturnGeneratedKey() =
        requireNotNull(asUpdateAndReturnGeneratedKey.runInSession(returnGeneratedKey = true)) {
            "Forventer at ${this.statement} ikke gir null"
        }
}

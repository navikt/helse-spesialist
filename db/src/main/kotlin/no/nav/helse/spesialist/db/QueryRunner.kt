package no.nav.helse.spesialist.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.action.QueryAction
import kotliquery.sessionOf
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQLWithQuestionMarks
import org.intellij.lang.annotations.Language
import java.sql.Array
import javax.sql.DataSource

interface QueryRunner {
    fun <T> Query.singleOrNull(mapping: (Row) -> T?): T?

    fun <T> Query.single(mapping: (Row) -> T): T

    fun <T> Query.list(mapping: (Row) -> T?): List<T>

    fun Query.update(): Int

    fun Query.updateAndReturnGeneratedKeyOrNull(): Long?

    fun Query.updateAndReturnGeneratedKey(): Long

    fun createArrayOf(
        typeName: String,
        map: Collection<Any>,
    ): Array
}

class MedSession(
    private val session: Session,
) : QueryRunner {
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

    override fun createArrayOf(
        typeName: String,
        map: Collection<Any>,
    ) = session.createArrayOf(typeName, map)
}

class MedDataSource(
    private val dataSource: DataSource,
) : QueryRunner {
    private fun <T> QueryAction<T>.runInSession(returnGeneratedKey: Boolean = false) = sessionOf(dataSource, returnGeneratedKey, strict = true).use(::runWithSession)

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

    override fun createArrayOf(
        typeName: String,
        map: Collection<Any>,
    ): Array = throw UnsupportedOperationException("Dette har vi ikke støtte for...")
}

class DataSourceDbQuery(
    private val dataSource: DataSource,
) : DbQuery {
    override fun <T> run(
        returnGeneratedKey: Boolean,
        block: () -> QueryAction<T>,
    ) = sessionOf(dataSource, returnGeneratedKey = returnGeneratedKey, strict = true).use { block().runWithSession(it) }
}

class SessionDbQuery(
    private val session: Session,
) : DbQuery {
    override fun <T> run(
        returnGeneratedKey: Boolean,
        block: () -> QueryAction<T>,
    ) = block().runWithSession(session)
}

interface DbQuery {
    fun <T> run(
        returnGeneratedKey: Boolean = false,
        block: () -> QueryAction<T>,
    ): T

    fun <T> single(
        @Language("PostgreSQL") sql: String,
        vararg params: Pair<String, Any?>,
        mapper: (Row) -> T?,
    ) = requireNotNull(
        run {
            asSQL(sql, *params).map(mapper).asSingle
        },
    ) { "Forventet å finne en rad i basen med spørringen '$sql' og parameterne ${params.joinToString()}" }

    fun <T> singleOrNull(
        @Language("PostgreSQL") sql: String,
        vararg params: Pair<String, Any?>,
        mapper: (Row) -> T?,
    ) = run { asSQL(sql, *params).map(mapper).asSingle }

    fun <T> list(
        @Language("PostgreSQL") sql: String,
        vararg params: Pair<String, Any?>,
        mapper: (Row) -> T?,
    ) = run { asSQL(sql, *params).map(mapper).asList }

    fun <T> listWithListParameter(
        @Language("PostgreSQL") sql: String,
        vararg params: Any,
        mapper: (Row) -> T?,
    ) = run { asSQLWithQuestionMarks(sql, *params).map(mapper).asList }

    fun update(
        @Language("PostgreSQL") sql: String,
        vararg params: Pair<String, Any?>,
    ) = run { asSQL(sql, *params).asUpdate }

    fun updateAndReturnGeneratedKey(
        @Language("PostgreSQL") sql: String,
        vararg params: Pair<String, Any?>,
    ) = run(returnGeneratedKey = true) { asSQL(sql, *params).asUpdateAndReturnGeneratedKey }

    fun execute(
        @Language("PostgreSQL") sql: String,
        vararg params: Pair<String, Any?>,
    ) = run { asSQL(sql, *params).asExecute }
}

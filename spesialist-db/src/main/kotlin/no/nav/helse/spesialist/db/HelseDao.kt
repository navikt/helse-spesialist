package no.nav.helse.spesialist.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

abstract class HelseDao(private val dataSource: DataSource) {
    companion object {
        fun asSQL(
            @Language("SQL") sql: String,
            vararg params: Pair<String, Any?>,
        ) = queryOf(sql, params.toMap())

        /**
         * Lager en string som kan gjøres om til et array ved å caste den til varchar[] i spørringer.
         *
         * For eksempel:
         * ```
         * i parameterene:
         *   "identer" to personidenter.somDbArray()
         * i spørringen:
         *   :identer::varchar[]
         * ```
         */
        fun <T> Iterable<T>?.somDbArray(transform: (T) -> CharSequence = { it.toString() }) =
            this?.joinToString(prefix = "{", postfix = "}", transform = transform) ?: "{}"

        fun insert(
            @Language("SQL") sql: String,
            params: Map<String, Any?>,
        ) = queryOf(sql, params)

        // Plis bare bruk denne til ting det ikke går an å gjøre med navngitte parametere - eks. ".. AND orgnummer = ANY(?)"
        fun asSQLWithQuestionMarks(
            @Language("SQL") sql: String,
            vararg params: Any?,
        ) = queryOf(sql, *params)

        fun <T> Query.single(
            session: Session,
            mapping: (Row) -> T?,
        ) = session.run(map(mapping).asSingle)

        fun <T> Query.list(
            session: Session,
            mapping: (Row) -> T?,
        ) = session.run(map(mapping).asList)

        fun Query.update(session: Session) = session.run(asUpdate)

        fun Query.updateAndReturnGeneratedKey(session: Session) = session.run(this.asUpdateAndReturnGeneratedKey)
    }

    fun <T> Query.single(mapping: (Row) -> T?) = sessionOf(dataSource, strict = true).use { single(it, mapping) }

    fun <T> Query.list(mapping: (Row) -> T?) = sessionOf(dataSource).use { list(it, mapping) }

    fun Query.update() = sessionOf(dataSource).use { update(it) }

    fun Query.updateAndReturnGeneratedKey() =
        sessionOf(
            dataSource,
            returnGeneratedKey = true,
        ).use { session -> session.run(this.asUpdateAndReturnGeneratedKey) }
}

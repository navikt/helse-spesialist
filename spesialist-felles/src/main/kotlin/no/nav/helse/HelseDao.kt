package no.nav.helse

import javax.sql.DataSource
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

abstract class HelseDao(private val dataSource: DataSource) {
    fun asSQL(@Language("SQL") sql: String, argMap: Map<String, Any?> = emptyMap()) = queryOf(sql, argMap)
    fun asSQL(@Language("SQL") sql: String, vararg params: Any?) = queryOf(sql, *params)

    fun <T> Query.single(mapping: (Row) -> T?) =
        sessionOf(dataSource, strict = true).use { session -> session.run(this.map { mapping(it) }.asSingle) }

    fun <T> Query.list(mapping: (Row) -> T?) =
        sessionOf(dataSource).use { session -> session.run(this.map { mapping(it) }.asList) }

    fun Query.update() = sessionOf(dataSource).use { session -> session.run(this.asUpdate) }
    fun Query.updateAndReturnGeneratedKey() = sessionOf(
        dataSource,
        returnGeneratedKey = true
    ).use { session -> session.run(this.asUpdateAndReturnGeneratedKey) }
}

package no.nav.helse

import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

abstract class HelseDao(private val dataSource: DataSource) {

    protected fun String.update(argMap: Map<String, Any> = emptyMap()) = sessionOf(dataSource).use { session ->
        session.run(queryOf(this, argMap).asUpdate)
    }

}

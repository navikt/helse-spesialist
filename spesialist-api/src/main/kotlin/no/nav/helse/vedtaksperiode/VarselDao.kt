package no.nav.helse.vedtaksperiode

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class VarselDao(private val dataSource: DataSource) {
    fun finnVarsler(vedtaksperiodeId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)"
        session.run(queryOf(statement, vedtaksperiodeId).map { it.string("melding") }.asList)
    }
}

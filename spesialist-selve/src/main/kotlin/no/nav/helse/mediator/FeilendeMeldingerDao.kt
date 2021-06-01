package no.nav.helse.mediator

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class FeilendeMeldingerDao(private val dataSource: DataSource) {
    internal fun lagre(id: UUID, eventName: String, blob: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO feilende_meldinger (id, event_name, blob) VALUES (?, ?, ?::json)"
        session.run(queryOf(statement, id, eventName, blob).asExecute)
    }
}

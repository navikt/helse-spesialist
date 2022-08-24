package no.nav.helse.mediator

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

/*
Denne tabellen holder på meldinger som spesialist ikke forstår seg på.
Det er ikke noen nåværende funksjonalitet for å gjøre noe med dem, men det er noe som kan legges til ved behov.
 */
internal class FeilendeMeldingerDao(private val dataSource: DataSource) {
    internal fun lagre(id: UUID, eventName: String, blob: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO feilende_meldinger (id, event_name, blob) VALUES (?, ?, ?::json) ON CONFLICT (id) DO NOTHING"
        session.run(queryOf(statement, id, eventName, blob).asExecute)
    }
}

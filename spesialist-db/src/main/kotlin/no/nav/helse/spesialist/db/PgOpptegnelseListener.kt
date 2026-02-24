package no.nav.helse.spesialist.db

import com.fasterxml.jackson.module.kotlin.readValue
import com.impossibl.postgres.api.jdbc.PGConnection
import com.impossibl.postgres.api.jdbc.PGNotificationListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import no.nav.helse.spesialist.application.OpptegnelseListener
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.intellij.lang.annotations.Language
import java.sql.Connection

data class OpptegnelseNotification(
    val personId: Long,
)

class PgOpptegnelseListener(
    connection: Connection,
) : OpptegnelseListener {
    private val connection = connection.unwrap(PGConnection::class.java)

    init {
        connection.createStatement().use { it.execute("LISTEN opptegnelse") }
    }

    private fun hentPersonId(identitetsnummer: Identitetsnummer): Long? {
        @Language("SQL")
        val query = "SELECT id FROM person WHERE fødselsnummer = ?"
        return connection.prepareStatement(query).use { stmt ->
            stmt.setString(1, identitetsnummer.value)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getLong("id") else null
            }
        }
    }

    override fun notifications(identitetsnummer: Identitetsnummer): Flow<Unit> {
        val personId = hentPersonId(identitetsnummer) ?: return emptyFlow()
        return callbackFlow {
            val listener =
                object : PGNotificationListener {
                    override fun notification(
                        processId: Int,
                        channelName: String?,
                        payload: String?,
                    ) {
                        val opptegnelse = payload?.let { objectMapper.readValue<OpptegnelseNotification?>(it) } ?: return
                        if (opptegnelse.personId == personId) trySend(Unit)
                    }
                }
            connection.addNotificationListener(listener)
            awaitClose { connection.removeNotificationListener(listener) }
        }
    }

    override fun close() {
        connection.close()
    }
}

package no.nav.helse.spesialist.db

import com.fasterxml.jackson.module.kotlin.readValue
import com.impossibl.postgres.api.jdbc.PGConnection
import com.impossibl.postgres.api.jdbc.PGNotificationListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import no.nav.helse.spesialist.application.OpptegnelseListener
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.intellij.lang.annotations.Language
import java.sql.Connection

typealias ConnectionProvider = () -> Connection

data class OpptegnelseNotification(
    val personId: Long,
)

class PgOpptegnelseListener(
    private val connectionProvider: ConnectionProvider,
) : OpptegnelseListener {
    override suspend fun onOpptegnelse(
        identitetsnummer: Identitetsnummer,
        block: suspend () -> Unit,
    ) {
        connectionProvider().unwrap(PGConnection::class.java).use { connection ->
            endringer(identitetsnummer, connection).collect { block() }
        }
    }

    private fun hentPersonId(
        identitetsnummer: Identitetsnummer,
        connection: Connection,
    ): Long? {
        @Language("SQL")
        val query = "SELECT id FROM person WHERE fødselsnummer = ?"
        return connection.prepareStatement(query).use { stmt ->
            stmt.setString(1, identitetsnummer.value)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getLong("id") else null
            }
        }
    }

    private fun endringer(
        identitetsnummer: Identitetsnummer,
        connection: PGConnection,
    ): Flow<Unit> {
        val personId = hentPersonId(identitetsnummer, connection) ?: return emptyFlow()
        return callbackFlow {
            val listener =
                object : PGNotificationListener {
                    override fun notification(
                        processId: Int,
                        channelName: String?,
                        payload: String?,
                    ) {
                        val opptegnelse = payload?.let { objectMapper.readValue<OpptegnelseNotification?>(it) } ?: return
                        if (opptegnelse.personId == personId) {
                            trySend(Unit).onFailure { throwable ->
                                loggError("Kunne ikke sende opptegnelse-notifikasjon til kanal", throwable)
                            }
                        }
                    }
                }
            connection.addNotificationListener(listener)
            connection.createStatement().use { it.execute("LISTEN opptegnelse") }
            awaitClose { connection.removeNotificationListener(listener) }
        }
    }
}

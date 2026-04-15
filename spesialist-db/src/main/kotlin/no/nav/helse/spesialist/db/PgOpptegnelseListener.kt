package no.nav.helse.spesialist.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.application.OpptegnelseListener
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.intellij.lang.annotations.Language
import org.postgresql.PGConnection
import java.sql.Connection
import javax.sql.DataSource

typealias ConnectionProvider = () -> Connection

data class OpptegnelseNotification(
    val personId: Long,
)

class PgOpptegnelseListener(
    scope: CoroutineScope,
    private val dataSource: DataSource,
    private val connectionProvider: ConnectionProvider,
) : OpptegnelseListener {

    // Single shared LISTEN connection — one blocked IO thread regardless of how many SSE clients
    // are connected. The upstream flow (and its connection) starts when the first subscriber
    // arrives and stops when the last one disconnects.
    private val sharedNotifications: SharedFlow<OpptegnelseNotification> =
        flow {
            val rawConnection = connectionProvider()
            try {
                val pgConnection = rawConnection.unwrap(PGConnection::class.java)
                (pgConnection as Connection).createStatement().use { it.execute("LISTEN opptegnelse") }
                while (currentCoroutineContext().isActive) {
                    // Blocks on the socket until a notification arrives or the timeout elapses.
                    // Notifications are delivered immediately (not polled) — the timeout only
                    // controls how often the loop checks for coroutine cancellation.
                    val pgNotifications =
                        withContext(Dispatchers.IO) { pgConnection.getNotifications(30_000) } ?: continue
                    for (notification in pgNotifications) {
                        val opptegnelse =
                            notification.parameter?.let { objectMapper.readValue<OpptegnelseNotification?>(it) }
                                ?: continue
                        emit(opptegnelse)
                    }
                }
            } finally {
                rawConnection.close()
            }
        }.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)

    override suspend fun onOpptegnelse(
        identitetsnummer: Identitetsnummer,
        block: suspend () -> Unit,
    ) {
        val personId = withContext(Dispatchers.IO) { hentPersonId(identitetsnummer) } ?: return
        sharedNotifications
            .filter { it.personId == personId }
            .collect { block() }
    }

    private fun hentPersonId(identitetsnummer: Identitetsnummer): Long? {
        @Language("SQL")
        val query = "SELECT id FROM person WHERE fødselsnummer = ?"
        return dataSource.connection.use { connection ->
            connection.prepareStatement(query).use { stmt ->
                stmt.setString(1, identitetsnummer.value)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong("id") else null
                }
            }
        }
    }
}

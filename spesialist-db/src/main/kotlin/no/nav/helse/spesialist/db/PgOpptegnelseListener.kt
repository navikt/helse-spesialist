package no.nav.helse.spesialist.db

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.application.OpptegnelseListener
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.intellij.lang.annotations.Language
import org.postgresql.PGConnection
import tools.jackson.module.kotlin.readValue
import java.sql.Connection
import javax.sql.DataSource
import kotlin.time.Duration.Companion.milliseconds

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
    //
    // The outer loop reconnects with backoff if the LISTEN connection dies (Postgres-failover,
    // dropped/killed connection, network blip). Uten dette ville en enkelt tilkoblingsfeil ha
    // drept hele SSE-varslingspipelinen permanent til neste app-restart.
    private val sharedNotifications: SharedFlow<OpptegnelseNotification> =
        flow {
            while (currentCoroutineContext().isActive) {
                try {
                    lyttOgEmitter()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    loggWarn("Mistet LISTEN-forbindelse mot database, kobler til på nytt om ${RECONNECT_DELAY_MS} ms", e)
                    delay(RECONNECT_DELAY_MS.milliseconds)
                }
            }
        }.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)

    private suspend fun FlowCollector<OpptegnelseNotification>.lyttOgEmitter() {
        val rawConnection = connectionProvider()
        try {
            val pgConnection = rawConnection.unwrap(PGConnection::class.java)
            (pgConnection as Connection).createStatement().use { it.execute("LISTEN opptegnelse") }
            while (currentCoroutineContext().isActive) {
                // Blocks on the socket until a notification arrives or the timeout elapses.
                // Notifications are delivered immediately (not polled) — the timeout only
                // controls how often the loop checks for coroutine cancellation.
                val pgNotifications =
                    withContext(Dispatchers.IO) { pgConnection.getNotifications(NOTIFICATION_TIMEOUT_MS) } ?: continue
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
    }

    override suspend fun onOpptegnelse(
        identitetsnummer: Identitetsnummer,
        block: suspend () -> Unit,
    ) {
        val personId = withContext(Dispatchers.IO) { hentPersonId(identitetsnummer) } ?: return
        sharedNotifications
            .filter { it.personId == personId }
            // conflate() decouples this subscriber from the shared upstream: en treg/hengende
            // SSE-klient kan ikke lenger blokkere varsling til de andre klientene. Trygt fordi
            // block() uansett henter alle opptegnelser etter sisteSekvensnummer, så sammenslåtte
            // signaler ikke medfører tap av data.
            .conflate()
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

    private companion object {
        private const val NOTIFICATION_TIMEOUT_MS = 10_000
        private const val RECONNECT_DELAY_MS = 5_000
    }
}

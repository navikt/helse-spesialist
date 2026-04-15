package no.nav.helse.spesialist.db

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.helse.spesialist.domain.Person
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class PgOpptegnelseListenerTest : AbstractDBIntegrationTest() {
    private val konfigurasjon = DBDBTestFixture.fixture.database.dbModuleConfiguration
    private val listenerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun nyListener(): PgOpptegnelseListener =
        PgOpptegnelseListener(
            scope = listenerScope,
            dataSource = dataSource,
            connectionProvider = {
                DriverManager.getConnection(konfigurasjon.jdbcUrl, konfigurasjon.username, konfigurasjon.password)
            },
        )

    @AfterEach
    fun stoppListener() {
        listenerScope.cancel()
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `leverer varsel til abonnent når NOTIFY mottas, og filtrerer bort andre personer`() =
        runBlocking {
            val person = opprettPerson()
            val personId = hentDbId(person)
            val listener = nyListener()

            val antallMottatt = AtomicInteger(0)
            val varsel = CompletableDeferred<Unit>()
            val abonnement =
                listenerScope.launch {
                    listener.onOpptegnelse(person.id) {
                        antallMottatt.incrementAndGet()
                        varsel.complete(Unit)
                    }
                }

            // Retry fordi LISTEN etableres asynkront når første abonnent kobler seg på.
            sendUntil(varsel) { sendNotify(personId) }
            assertTrue(varsel.isCompleted, "Abonnenten skulle ha mottatt varsel for sin egen person")

            // LISTEN er nå aktiv: varsel for en annen person skal filtreres bort.
            val antallEtterEgenPerson = antallMottatt.get()
            repeat(3) { sendNotify(personId + 1) }
            delay(1_000.milliseconds)
            assertEquals(
                antallEtterEgenPerson,
                antallMottatt.get(),
                "Abonnenten skulle ikke motta varsel for en annen person",
            )

            abonnement.cancel()
        }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    fun `kobler til på nytt og fortsetter å levere varsler etter at LISTEN-forbindelsen dør`() =
        runBlocking {
            val person = opprettPerson()
            val personId = hentDbId(person)
            val listener = nyListener()

            val førsteVarsel = CompletableDeferred<Unit>()
            val andreVarsel = CompletableDeferred<Unit>()
            // Åpnes først etter at forbindelsen er drept, slik at kun et varsel levert etter
            // reconnect kan fullføre andreVarsel — ikke et etterslep fra før forbindelsen døde.
            val reconnectFase = AtomicBoolean(false)
            val abonnement =
                listenerScope.launch {
                    listener.onOpptegnelse(person.id) {
                        when {
                            !førsteVarsel.isCompleted -> førsteVarsel.complete(Unit)
                            reconnectFase.get() -> andreVarsel.complete(Unit)
                        }
                    }
                }

            sendUntil(førsteVarsel) { sendNotify(personId) }

            // Dreper backend-forbindelsen som holder LISTEN, slik at getNotifications kaster.
            terminerListenBackend()
            reconnectFase.set(true)

            // Etter reconnect-backoff skal listeneren koble til på nytt og levere neste varsel.
            sendUntil(andreVarsel, timeoutMs = 45_000) { sendNotify(personId) }

            assertTrue(andreVarsel.isCompleted, "Listeneren skulle ha koblet til på nytt og levert varsel")
            abonnement.cancel()
        }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    fun `én treg abonnent blokkerer ikke varsling til andre abonnenter`() =
        runBlocking {
            val tregPerson = opprettPerson()
            val raskPerson = opprettPerson()
            val tregPersonId = hentDbId(tregPerson)
            val raskPersonId = hentDbId(raskPerson)
            val listener = nyListener()

            val tregStartet = CompletableDeferred<Unit>()
            val slippTreg = CompletableDeferred<Unit>()
            val raskMottatt = CompletableDeferred<Unit>()

            val tregAbonnement =
                listenerScope.launch {
                    listener.onOpptegnelse(tregPerson.id) {
                        tregStartet.complete(Unit)
                        // Simulerer en treg/hengende SSE-klient.
                        slippTreg.await()
                    }
                }
            val raskAbonnement =
                listenerScope.launch {
                    listener.onOpptegnelse(raskPerson.id) { raskMottatt.complete(Unit) }
                }

            // Få den trege abonnenten inn i block() slik at den henger.
            sendUntil(tregStartet) { sendNotify(tregPersonId) }

            // Fyll opp den delte flytens buffer med varsler den hengende abonnenten aldri rekker å
            // konsumere. Uten frikobling (conflate) vil den delte upstream-en da suspendere på emit,
            // og dermed stoppe varsling til ALLE abonnenter — inkludert den raske.
            repeat(200) { sendNotify(tregPersonId) }
            delay(500.milliseconds)

            // Den raske abonnenten skal fortsatt motta varsel selv om den trege henger.
            sendUntil(raskMottatt, timeoutMs = 10_000) { sendNotify(raskPersonId) }

            assertTrue(raskMottatt.isCompleted, "Rask abonnent skulle ikke bli blokkert av treg abonnent")

            slippTreg.complete(Unit)
            tregAbonnement.cancel()
            raskAbonnement.cancel()
        }

    private suspend fun sendUntil(
        signal: CompletableDeferred<Unit>,
        timeoutMs: Long = 20_000,
        send: () -> Unit,
    ) {
        withTimeout(timeoutMs.milliseconds) {
            while (!signal.isCompleted) {
                send()
                delay(200.milliseconds)
            }
        }
    }

    private fun sendNotify(personId: Long) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT pg_notify('opptegnelse', ?)").use { stmt ->
                stmt.setString(1, """{"personId":$personId}""")
                stmt.execute()
            }
        }
    }

    private fun terminerListenBackend() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute(
                    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                        "WHERE query = 'LISTEN opptegnelse' AND pid <> pg_backend_pid()",
                )
            }
        }
    }

    private fun hentDbId(person: Person): Long =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT id FROM person WHERE fødselsnummer = ?").use { stmt ->
                stmt.setString(1, person.id.value)
                stmt.executeQuery().use { rs ->
                    check(rs.next()) { "Fant ikke person i databasen" }
                    rs.getLong("id")
                }
            }
        }
}

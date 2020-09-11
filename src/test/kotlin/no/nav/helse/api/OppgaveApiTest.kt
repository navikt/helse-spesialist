package no.nav.helse.api

import AbstractEndToEndTest
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.TestPerson
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto
import no.nav.helse.modell.vedtak.SaksbehandleroppgavereferanseDto
import no.nav.helse.tildeling.opprettSaksbehandler
import no.nav.helse.tildeling.opprettSaksbehandlerOppgave
import no.nav.helse.tildeling.opprettTildeling
import no.nav.helse.tildeling.opprettVedtak
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.*
import kotlin.test.assertEquals

class OppgaveApiTest : AbstractEndToEndTest() {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private lateinit var oppgaveMediator: OppgaveMediator

    @BeforeAll
    fun setup() {
        oppgaveMediator = OppgaveMediator(OppgaveDao(dataSource), VedtakDao(dataSource))
        embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            routing {
                oppgaveApi(oppgaveMediator)
                direkteOppgaveApi(oppgaveMediator)
            }
        }.also {
            it.start(wait = false)
        }
    }


    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort
        }
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
    }

    @Test
    fun `finner oppgave for person via fødselsnummer`() {
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)

        person.sendGodkjenningMessage(eventId)
        person.sendPersoninfo(eventId)

        val referanse = runBlocking {
            client.get<SaksbehandleroppgavereferanseDto>("/api/v1/oppgave") {
                header("fodselsnummer", person.fødselsnummer)
            }
        }

        assertEquals(eventId, referanse.oppgavereferanse)
    }

    @Test
    fun `får 404 når oppgaven ikke finnes`() {
        val response = runBlocking {
            client.get<HttpStatement>("/api/v1/oppgave") {
                header("fodselsnummer", "42069")
            }.execute()
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `får med tildelinger når man henter oppgaver`() {
        val saksbehandlerreferanse = UUID.randomUUID()
        val oppgavereferanse = UUID.randomUUID()
        val vedtakId = dataSource.opprettVedtak()
        val epost = "sara.saksbehandler@nav.no"
        dataSource.opprettSaksbehandler(saksbehandlerreferanse, epost)
        dataSource.opprettSaksbehandlerOppgave(oppgavereferanse, vedtakId)
        dataSource.opprettTildeling(oppgavereferanse, saksbehandlerreferanse)

        val oppgaver = runBlocking {
            client.get<List<SaksbehandleroppgaveDto>>("/api/oppgaver")
        }
        assertEquals(epost, oppgaver.find { it.oppgavereferanse == oppgavereferanse }?.saksbehandlerepost)
    }
}

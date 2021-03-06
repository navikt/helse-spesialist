package no.nav.helse.mediator.api

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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveMediator
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.assertThrows
import java.net.ServerSocket
import java.util.*

@TestInstance(Lifecycle.PER_CLASS)
internal class OppgaveApiTest {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    @BeforeAll
    fun setup() {
        embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            routing {
                oppgaveApi(oppgaveMediator, UUID.randomUUID().toString())
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
    fun `returnerer 400 bad request hvis fødselsnummer ikke er satt eller er null`() {
        assertThrows<ClientRequestException> {
            runBlocking {
                client.get<HttpStatement>("/api/v1/oppgave") { header("fodselsnummer", null) }.execute()
            }
        }
    }

    @Test
    fun `får 404 not found hvis oppgaven ikke finnes`() {
        every { oppgaveMediator.hentOppgaveId(any()) } returns null
        assertThrows<ClientRequestException> {
            runBlocking {
                client.get<HttpStatement>("/api/v1/oppgave") { header("fodselsnummer", "42069") }.execute()
            }
        }
    }
}

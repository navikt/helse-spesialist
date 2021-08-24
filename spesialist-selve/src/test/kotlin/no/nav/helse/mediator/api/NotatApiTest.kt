package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.notat.NotatDto
import no.nav.helse.notat.NotatMediator
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.*
import io.ktor.http.HttpStatusCode.Companion as HttpStatusCode1

@TestInstance(Lifecycle.PER_CLASS)
internal class NotatApiTest {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val notatMediator = mockk<NotatMediator>(relaxed = true)

    @BeforeAll
    fun setup() {
        embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            routing {
                notaterApi(notatMediator)
            }
        }.also {
            it.start(wait = false)
        }

        every { notatMediator.finn(listOf(1, 2)) } returns mapOf(
            1 to listOf(NotatDto(
                id = 1,
                tekst = "yo!",
                opprettet = LocalDateTime.now(),
                saksbehandlerOid = UUID.randomUUID(),
                saksbehandlerNavn = "per",
                saksbehandlerEpost = "noen@example.com",
                oppgaveRef = 1)),
            2 to listOf(NotatDto(
                id = 1,
                tekst = "sup?",
                opprettet = LocalDateTime.now(),
                saksbehandlerOid = UUID.randomUUID(),
                saksbehandlerNavn = "per",
                saksbehandlerEpost = "noen@example.com",
                oppgaveRef = 2))
        )
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
    fun `flere query params med samme navn er lov`() = runBlocking{
        val response = client.get<Map<Int, List<NotatDto>>>("/api/notater?oppgave_ref=1&oppgave_ref=2")
        assertEquals(2, response.size)
    }

}

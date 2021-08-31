package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.*

@TestInstance(Lifecycle.PER_CLASS)
internal class NotatApiTest {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val notatMediator = mockk<NotatMediator>(relaxed = true)

    private val vedtaksperiodeId1 = "8624dbae-0c42-445b-a869-a3023e6ca3f7"
    private val vedtaksperiodeId2 = "8042174b-5ab5-425f-b427-e7905cb8bea9"

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

        val periode_1_id = UUID.fromString(vedtaksperiodeId1)
        val periode_2_id = UUID.fromString(vedtaksperiodeId2)
        every { notatMediator.finn(listOf(periode_1_id, periode_2_id)) } returns mapOf(
            periode_1_id to listOf(NotatDto(
                id = 1,
                tekst = "yo!",
                opprettet = LocalDateTime.now(),
                saksbehandlerOid = UUID.randomUUID(),
                saksbehandlerNavn = "per",
                saksbehandlerEpost = "noen@example.com",
                vedtaksperiodeId = periode_1_id)),
            periode_2_id to listOf(NotatDto(
                id = 2,
                tekst = "sup?",
                opprettet = LocalDateTime.now(),
                saksbehandlerOid = UUID.randomUUID(),
                saksbehandlerNavn = "per",
                saksbehandlerEpost = "noen@example.com",
                vedtaksperiodeId = periode_2_id))
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
        val response = client.get<Map<UUID, List<NotatDto>>>("/api/notater?vedtaksperiode_id=$vedtaksperiodeId1&vedtaksperiode_id=$vedtaksperiodeId2")
        assertEquals(2, response.size)
    }

}

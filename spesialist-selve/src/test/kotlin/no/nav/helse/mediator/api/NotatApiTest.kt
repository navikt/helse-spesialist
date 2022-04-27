package no.nav.helse.mediator.api

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.notat.NotatDto
import no.nav.helse.notat.NotatMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertTrue

@TestInstance(Lifecycle.PER_CLASS)
internal class NotatApiTest: AbstractApiTest() {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private lateinit var notatMediator: NotatMediator

    private val vedtaksperiodeId1 = "8624dbae-0c42-445b-a869-a3023e6ca3f7"
    private val vedtaksperiodeId2 = "8042174b-5ab5-425f-b427-e7905cb8bea9"
    private val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val notatId = 1

    @BeforeAll
    fun setup() {
        notatMediator = mockk<NotatMediator>(relaxed = true)
        setupServer {
            notaterApi(notatMediator)
        }
    }

    @BeforeEach
    fun set() {
        clearMocks(notatMediator)
    }

    @Test
    fun `post av notat`() {
        val response = runBlocking {
            client.preparePost("/api/notater/$vedtaksperiodeId1") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf("tekst" to "en-tekst"))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            notatMediator.lagre(UUID.fromString(vedtaksperiodeId1), "en-tekst", SAKSBEHANDLER_OID)
        }
    }

    @Test
    fun `feilregistrering av notat`() {
        every { notatMediator.feilregistrer(notatId, SAKSBEHANDLER_OID)} returns true
        val response = runBlocking {
            client.preparePut("/api/notater/$vedtaksperiodeId1/feilregistrer/$notatId") {
                accept(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            notatMediator.feilregistrer(notatId, SAKSBEHANDLER_OID)
        }
    }

    @Test
    fun `feilregistrering av annen saksbehandler sitt originale notat`() {
        every { notatMediator.feilregistrer(notatId, SAKSBEHANDLER_OID)} returns false
        val response = runBlocking {
            client.preparePut("/api/notater/$vedtaksperiodeId1/feilregistrer/$notatId") {
                accept(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        assertEquals(
            response.status,
            HttpStatusCode.BadRequest,
            "HTTP response burde gi Forbidden, fikk ${response.status}"
        )
    }

    @Test
    fun `manglende vedtaksperiode på post gir trøbbel`() {
        val response = runBlocking {
            client.preparePost("/api/notater/null") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf("tekst" to "en-tekst"))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }
        assertEquals(
            response.status,
            HttpStatusCode.InternalServerError,
            "HTTP response burde gi Internal Server Error, fikk ${response.status}"
        )
    }

    @Test
    fun `flere query params med samme navn er lov`() = runBlocking{
        val periode_1_id = UUID.fromString(vedtaksperiodeId1)
        val periode_2_id = UUID.fromString(vedtaksperiodeId2)
        every { notatMediator.finn(listOf(periode_1_id, periode_2_id)) } returns mapOf(
            periode_1_id to listOf(NotatDto(
                id = 1,
                tekst = "yo!",
                opprettet = LocalDateTime.now(),
                saksbehandlerOid = SAKSBEHANDLER_OID,
                saksbehandlerNavn = "per",
                saksbehandlerEpost = "noen@example.com",
                vedtaksperiodeId = periode_1_id,
                feilregistrert = false,
                feilregistrert_tidspunkt = null)),
            periode_2_id to listOf(NotatDto(
                id = 2,
                tekst = "sup?",
                opprettet = LocalDateTime.now(),
                saksbehandlerOid = SAKSBEHANDLER_OID,
                saksbehandlerNavn = "per",
                saksbehandlerEpost = "noen@example.com",
                vedtaksperiodeId = periode_2_id,
                feilregistrert = false,
                feilregistrert_tidspunkt = null))
        )
        val response = client.prepareGet("/api/notater?vedtaksperiode_id=$vedtaksperiodeId1&vedtaksperiode_id=$vedtaksperiodeId2"){
            authentication(SAKSBEHANDLER_OID)
        }.execute().body<Map<UUID, List<NotatDto>>>()
        assertEquals(2, response.size)
    }

}

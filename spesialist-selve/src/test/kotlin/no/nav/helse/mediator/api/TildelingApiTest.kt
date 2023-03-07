package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.modell.tildeling.TildelingService
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.feilhåndtering.FeilDto
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import kotlin.random.Random.Default.nextLong

@TestInstance(PER_CLASS)
internal class TildelingApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var tildelingService: TildelingService

    @BeforeAll
    fun setupTildeling() {
        tildelingService = mockk(relaxed = true)
        setupServer {
            tildelingApi(tildelingService)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(tildelingService)
    }

    @Test
    fun `kan tildele en oppgave til seg selv`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post("/api/tildeling/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.createObjectNode())
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            tildelingService.tildelOppgaveTilSaksbehandler(oppgavereferanse, SAKSBEHANDLER_OID, any(), any(), any(), any())
        }
    }

    @Test
    fun `kan slette en tildeling av en oppgave`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.delete("/api/tildeling/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.createObjectNode())
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            tildelingService.fjernTildeling(oppgavereferanse)
        }
    }

    @Test
    fun `Gir feil hvis bruker forsøker å tildele en oppgave som allerede er tildelt`() {
        val tildeltFeil = OppgaveAlleredeTildelt(TildelingApiDto(epost = "annenSaksbehandler@nav.no", oid = UUID.randomUUID(), påVent = false, navn = "en annen saksbehandler"))
        every { tildelingService.tildelOppgaveTilSaksbehandler(any(), any(), any(), any(), any(), any()) } throws tildeltFeil
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post("/api/tildeling/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.createObjectNode())
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val feilDto = runBlocking { response.body<FeilDto>() }
        assertEquals( "oppgave_er_allerede_tildelt", feilDto.feilkode)
        val tildeling = objectMapper.valueToTree<JsonNode>(feilDto.kontekst["tildeling"])
        assertEquals("en annen saksbehandler", tildeling["navn"].asText())
        assertEquals("spesialist", feilDto.kildesystem)
    }

    @Test
    fun `manglende oppgavereferanse POST gir Bad Request`() {
        val response = runBlocking {
            client.post("/api/tildeling/null") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.createObjectNode())
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertEquals(response.status, HttpStatusCode.BadRequest, "HTTP response burde returnere en Bad request, fikk ${response.status}")
    }

    @Test
    fun `manglende oppgavereferanse DELETE gir Bad Request`() {
        val response = runBlocking {
            client.delete("/api/tildeling/null") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.createObjectNode())
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(response.status, HttpStatusCode.BadRequest, "HTTP response burde returnere en Bad request, fikk ${response.status}")
    }
}

package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.feilhåndtering.FeilDto
import no.nav.helse.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.objectMapper
import no.nav.helse.tildeling.TildelingApiDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.util.*
import kotlin.random.Random.Default.nextLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
internal class TildelingApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var tildelingMediator: TildelingMediator

    @BeforeAll
    fun setupTildeling() {
        tildelingMediator = mockk(relaxed = true)
        setupServer {
            tildelingApi(tildelingMediator)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(tildelingMediator)
    }

    @Test
    fun `kan tildele en oppgave til seg selv`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post<HttpResponse>("/api/tildeling/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            tildelingMediator.tildelOppgaveTilSaksbehandler(oppgavereferanse, SAKSBEHANDLER_OID, any(), any(), any())
        }
    }

    @Test
    fun `kan slette en tildeling av en oppgave`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.delete<HttpResponse>("/api/tildeling/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            tildelingMediator.fjernTildeling(oppgavereferanse)
        }
    }

    @Test
    fun `Gir feil hvis bruker forsøker å tildele en oppgave som allerede er tildelt`() {
        val tildeltFeil = OppgaveAlleredeTildelt(TildelingApiDto(epost = "annenSaksbehandler@nav.no", oid = UUID.randomUUID(), påVent = false, navn = "en annen saksbehandler"))
        every { tildelingMediator.tildelOppgaveTilSaksbehandler(any(), any(), any(), any(), any()) } throws tildeltFeil
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post<HttpResponse>("/api/tildeling/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val feilDto = runBlocking { response.receive<FeilDto>() }
        assertEquals( "oppgave_er_allerede_tildelt", feilDto.feilkode)
        val tildeling = objectMapper.valueToTree<JsonNode>(feilDto.kontekst["tildeling"])
        assertEquals("en annen saksbehandler", tildeling["navn"].asText())
        assertEquals("spesialist", feilDto.kildesystem)
    }

    @Test
    fun `manglende oppgavereferanse POST gir Bad Request`() {
        val response = runBlocking {
            client.post<HttpResponse>("/api/tildeling/null") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertEquals(response.status, HttpStatusCode.BadRequest, "HTTP response burde returnere en Bad request, fikk ${response.status}")
    }

    @Test
    fun `manglende oppgavereferanse DELETE gir Bad Request`() {
        val response = runBlocking {
            client.delete<HttpResponse>("/api/tildeling/null") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(response.status, HttpStatusCode.BadRequest, "HTTP response burde returnere en Bad request, fikk ${response.status}")
    }
}

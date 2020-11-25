package no.nav.helse.mediator.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.modell.feilhåndtering.FeilDto
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random.Default.nextLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TildelingApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var tildelingMediator: TildelingMediator

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
            tildelingMediator.tildelOppgaveTilSaksbehandler(
                oppgavereferanse,
                SAKSBEHANDLER_OID,
                any(),
                any()
            )
        }
    }

    @Test
    fun `Gir feil hvis bruker forsøker å tildele en oppgave som allerede er tildelt`() {
        every { tildelingMediator.tildelOppgaveTilSaksbehandler(any(), any(), any(), any()) } throws ModellFeil(
            OppgaveErAlleredeTildelt("en annen saksbehandler")
        )
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
        assertEquals(feilDto.feilkode, OppgaveErAlleredeTildelt("").feilkode)
        assertEquals("en annen saksbehandler", feilDto.kontekst["tildeltTil"])
    }

    @Test
    fun `manglende oppgavereferanse gir Bad Request`() {
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
}

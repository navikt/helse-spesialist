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
import no.nav.helse.feilhåndtering.FeilDto
import no.nav.helse.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.modell.leggpåvent.LeggPåVentMediator
import no.nav.helse.objectMapper
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
internal class LeggPåVentApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var leggPåVentMediator: LeggPåVentMediator

    @BeforeAll
    fun setupTildeling() {
        leggPåVentMediator = mockk(relaxed = true)
        setupServer {
            leggPåVentApi(leggPåVentMediator)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(leggPåVentMediator)
    }

    @Test
    fun `kan legge en oppgave på vent`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post<HttpResponse>("/api/leggpaavent/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            leggPåVentMediator.leggOppgavePåVent(oppgavereferanse)
        }
    }

    @Test
    fun `kan fjerne på vent`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.delete<HttpResponse>("/api/leggpaavent/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            leggPåVentMediator.fjernPåVent(oppgavereferanse)
        }
    }

    @Test
    fun `gir feil hvis bruker forsøker å legge en oppgave på vent før den er tildelt bruker`() {
        every { leggPåVentMediator.leggOppgavePåVent(any()) } throws OppgaveIkkeTildelt(1L)
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post<HttpResponse>("/api/leggpaavent/${oppgavereferanse}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }

        assertEquals(HttpStatusCode.FailedDependency, response.status)
        val feil = runBlocking { response.receive<FeilDto>() }
        assertEquals("oppgave_er_ikke_tildelt", feil.feilkode)
    }

    @Test
    fun `manglende oppgavereferanse POST gir Bad Request`() {
        val response = runBlocking {
            client.post<HttpResponse>("/api/leggpaavent/null") {
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
            client.delete<HttpResponse>("/api/leggpaavent/null") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(response.status, HttpStatusCode.BadRequest, "HTTP response burde returnere en Bad request, fikk ${response.status}")
    }
}

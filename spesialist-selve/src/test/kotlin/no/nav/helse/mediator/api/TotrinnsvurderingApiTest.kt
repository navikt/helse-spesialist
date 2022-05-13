package no.nav.helse.mediator.api

import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.oppgave.OppgaveMediator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TotrinnsvurderingApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var oppgaveMediator: OppgaveMediator

    private val totrinnsvurderingUrlPath = "/api/totrinnsvurdering"

    @BeforeAll
    fun setupTildeling() {
        oppgaveMediator = mockk(relaxed = true)
        setupServer {
            totrinnsvurderingApi(oppgaveMediator)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(oppgaveMediator)
    }

    @Test
    fun totrinnsvurderingOk() {
        val expectedOKHttpStatusCode = HttpStatusCode.OK.value
        val oppgaveId = 1L

        val response = runBlocking {
            client.preparePost("$totrinnsvurderingUrlPath/$oppgaveId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 1) {
            oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any())
        }

        Assertions.assertEquals(expectedOKHttpStatusCode, response.status.value)
    }

    @Test
    fun totrinnsvurderingManglerOppgaveIdIPath() {

        val expectedBadRequestHttpStatusCode = HttpStatusCode.NotFound.value

        val response = runBlocking {
            client.preparePost(totrinnsvurderingUrlPath) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 0) {
            oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any())
        }
        assertEquals(expectedBadRequestHttpStatusCode, response.status.value)
    }

    @Test
    fun totrinnsvurderingManglerAccessToken() {

        val expectedUnauthorizedHttpStatusCode = HttpStatusCode.Unauthorized.value
        val oppgaveId = 1L

        val response = runBlocking {
            client.preparePost("$totrinnsvurderingUrlPath/$oppgaveId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }.execute()
        }

        verify(exactly = 0) {
            oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any())
        }
        assertEquals(expectedUnauthorizedHttpStatusCode, response.status.value)
    }

}
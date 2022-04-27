package no.nav.helse.mediator.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.mockk.every
import io.mockk.mockk
import java.net.ServerSocket
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.AzureAdAppConfig
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.mediator.api.AbstractApiTest.Companion.clientId
import no.nav.helse.mediator.api.AbstractApiTest.Companion.issuer
import no.nav.helse.mediator.api.AbstractApiTest.Companion.jwtStub
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveMediator
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import kotlin.test.assertEquals
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

@TestInstance(Lifecycle.PER_CLASS)
internal class OppgaveApiTest {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val KODE7_SAKSBEHANDLER_GROUP = "c21e58b6-d838-4cb0-919a-398cd40117e3"

    @BeforeAll
    fun setup() {
        embeddedServer(CIO, port = httpPort) {
            install(ContentNegotiationServer) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            val jwkProvider = jwtStub.getJwkProviderMock()
            val azureConfig = AzureAdAppConfig(
                clientId = clientId,
                issuer = issuer,
                jwkProvider = jwkProvider
            )
            azureAdAppAuthentication(azureConfig)
            routing {
                authenticate("oidc") {
                    oppgaveApi(
                        oppgaveMediator = oppgaveMediator,
                        riskSupersaksbehandlergruppe = UUID.randomUUID(),
                        kode7Saksbehandlergruppe = UUID.randomUUID()
                    )
                }
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
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }
    }

    @Test
    fun `returnerer 404 not found hvis fødselsnummer ikke er satt eller er null`() {
        val response = runBlocking {
                client.prepareGet("/api/v1/oppgave") { header("fodselsnummer", null) }.execute()
            }
        assertEquals(response.status, HttpStatusCode.NotFound)
    }

    @Test
    fun `får 404 not found hvis oppgaven ikke finnes`() {
        every { oppgaveMediator.hentOppgaveId(any()) } returns null
        val response = runBlocking {
                client.prepareGet("/api/v1/oppgave") { header("fodselsnummer", "42069")
                }.execute()
            }
        assertEquals(response.status, HttpStatusCode.NotFound)
    }

}

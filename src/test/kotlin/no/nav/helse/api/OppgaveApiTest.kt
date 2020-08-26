package no.nav.helse.api

import AbstractEndToEndTest
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.client.statement.HttpStatement
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import no.nav.helse.*
import no.nav.helse.modell.vedtak.SaksbehandleroppgavereferanseDto
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.*
import kotlin.test.assertEquals

class OppgaveApiTest: AbstractEndToEndTest() {
    private val httpPort = ServerSocket(0).use { it.localPort }

    private val jwtStub = JwtStub()
    private val clientId = "client_id"
    private val speilClientId = "speil_id"
    private val issuer = "https://jwt-provider-domain"
    private val requiredGroup = "required_group"

    private val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
    private val azureConfig = AzureAdAppConfig(clientId = clientId, speilClientId = speilClientId, requiredGroup = requiredGroup)
    private val jwkProvider = jwtStub.getJwkProviderMock()
    private lateinit var oppgaveMediator: OppgaveMediator

    @BeforeAll
    fun setup() {
        oppgaveMediator = OppgaveMediator(dataSource)
        embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)
            oppgaveApi(oppgaveMediator)
        }.also {
            it.start(wait = false)
        }
    }


    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort
            header(
                "Authorization",
                "Bearer ${jwtStub.getToken(
                    arrayOf(requiredGroup),
                    UUID.randomUUID().toString(),
                    "tbd@nav.no",
                    speilClientId,
                    issuer
                )}"
            )
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }



    @Test
    fun `finner oppgave for person via fødselsnummer`() {
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)

        person.sendGodkjenningMessage(eventId)
        person.sendPersoninfo(eventId)

        val referanse = runBlocking {
            client.get<SaksbehandleroppgavereferanseDto>("/api/v1/oppgave") {
                header("fodselsnummer", person.fødselsnummer)
            }
        }

        assertEquals(eventId, referanse.oppgavereferanse)
    }

    @Test
    fun `får 404 når oppgaven ikke finnes`() {
        val response = runBlocking {
            client.get<HttpStatement>("/api/v1/oppgave") {
                header("fodselsnummer", "42069")
            }.execute()
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}

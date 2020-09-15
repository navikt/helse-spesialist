package no.nav.helse.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.AzureAdAppConfig
import no.nav.helse.OidcDiscovery
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.modell.feilhåndtering.FeilDto
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.objectMapper
import no.nav.helse.tildeling.TildelingMediator
import org.junit.jupiter.api.*
import java.net.ServerSocket
import java.util.*
import kotlin.random.Random.Default.nextLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TildelingApiTest {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var tildelingMediator: TildelingMediator

    @Test
    fun `kan tildele en oppgave til seg selv`() {
        val oppgavereferanse = nextLong()
        val response = runBlocking {
            client.post<HttpResponse>("/api/v1/tildeling/${oppgavereferanse}") {
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
            client.post<HttpResponse>("/api/v1/tildeling/${oppgavereferanse}") {
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

    private fun HttpRequestBuilder.authentication(oid: UUID) {
        header(
            "Authorization",
            "Bearer ${
                jwtStub.getToken(
                    arrayOf(requiredGroup),
                    oid.toString(),
                    "epostadresse",
                    clientId,
                    issuer
                )
            }"
        )
    }

    private lateinit var server: ApplicationEngine
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val jwtStub = JwtStub()
    private val requiredGroup = "required_group"
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"
    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort

        }
        expectSuccess = false
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
    }

    @BeforeAll
    fun setup() {
        tildelingMediator = mockk(relaxed = true)

        server = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }

            val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
            val azureConfig =
                AzureAdAppConfig(
                    clientId = UUID.randomUUID().toString(),
                    speilClientId = clientId,
                    requiredGroup = requiredGroup
                )
            val jwkProvider = jwtStub.getJwkProviderMock()
            azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)

            routing {
                authenticate("saksbehandler-direkte") {
                    tildelingApi(tildelingMediator)
                }
            }
        }.also {
            it.start(wait = false)
        }
    }

    @AfterAll
    fun tearDown() {
        server.stop(1000, 1000)
    }

    @AfterEach
    fun tearDownEach () {
        clearMocks(tildelingMediator)
    }
}

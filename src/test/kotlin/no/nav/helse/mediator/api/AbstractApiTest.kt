package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
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
import kotlinx.coroutines.runBlocking
import no.nav.helse.AzureAdAppConfig
import no.nav.helse.OidcDiscovery
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.objectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import java.net.ServerSocket
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractApiTest {

    private lateinit var server: TestServer
    protected lateinit var client: HttpClient

    protected fun setupServer(block: Route.() -> Unit) {
        server = TestServer( build=block ).also { it.start() }
        client = server.restClient()
    }

    @AfterAll
    protected fun tearDown() { server.stop() }

    companion object {
        private const val requiredGroup = "required_group"
        private const val clientId = "client_id"
        private const val epostadresse = "sara.saksbehandler@nav.no";
        private const val issuer = "https://jwt-provider-domain"
        internal val jwtStub = JwtStub()

        fun HttpRequestBuilder.authentication(oid: UUID) {
            header(
                "Authorization",
                "Bearer ${
                    jwtStub.getToken(
                        arrayOf(requiredGroup),
                        oid.toString(),
                        epostadresse,
                        clientId,
                        issuer
                    )
                }"
            )
        }
    }

    class TestServer(
        private val httpPort: Int = ServerSocket(0).use { it.localPort },
        private val build: Route.() -> Unit
    ) {
        private var server: NettyApplicationEngine? = null

        fun start() {
            server = createAuthenticatedServer(httpPort, this.build)
            server?.start(wait = false)
        }
        fun stop() { server?.stop(1000, 1000) }

        fun <T> withAuthenticatedServer(test: suspend (HttpClient) -> T): T {
            return runBlocking {
                var localServer: NettyApplicationEngine? = null;
                try {
                    localServer = createAuthenticatedServer(httpPort, build)
                    localServer.start(wait = false)

                    test(restClient())
                } finally {
                    localServer?.stop(1000, 1000)
                }
            }
        }

        private fun createAuthenticatedServer(httpPort: Int, build: Route.() -> Unit): NettyApplicationEngine {
            return embeddedServer(Netty, port = httpPort) {
                install(ContentNegotiation) {
                    register(
                        ContentType.Application.Json,
                        JacksonConverter(objectMapper)
                    )
                }
                val oidcDiscovery =
                    OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
                val azureConfig =
                    AzureAdAppConfig(
                        clientId = UUID.randomUUID().toString(),
                        speilClientId = clientId,
                        requiredGroup = requiredGroup
                    )
                val jwkProvider = jwtStub.getJwkProviderMock()
                azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)
                routing {
                    authenticate("saksbehandler-direkte", build = build)
                }
            }
        }

        fun restClient(): HttpClient {
            return HttpClient {
                defaultRequest {
                    host = "localhost"
                    port = httpPort
                }
                expectSuccess = false
                install(JsonFeature) {
                    serializer = JacksonSerializer(jackson = objectMapper)
                }
            }
        }
    }
}

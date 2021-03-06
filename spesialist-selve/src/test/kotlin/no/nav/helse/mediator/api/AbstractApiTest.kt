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
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.objectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import java.net.ServerSocket
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractApiTest {

    private lateinit var server: TestServerRuntime
    protected lateinit var client: HttpClient

    protected fun setupServer(λ: Route.() -> Unit) {
        server = TestServer(λ = λ).start()
        client = server.restClient()
    }

    @AfterAll
    protected fun tearDown() {
        server.close()
    }

    companion object {
        private val requiredGroup: UUID = UUID.randomUUID()
        private const val clientId = "client_id"
        private const val epostadresse = "sara.saksbehandler@nav.no";
        private const val issuer = "https://jwt-provider-domain"
        internal val jwtStub = JwtStub()

        fun HttpRequestBuilder.authentication(oid: UUID, group: String? = null) {
            header(
                "Authorization",
                "Bearer ${
                    jwtStub.getToken(
                        listOfNotNull(requiredGroup.toString(), group),
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
        private val λ: Route.() -> Unit,
    ) {
        fun start(): TestServerRuntime {
            return TestServerRuntime(λ, httpPort)
        }

        fun <T> withAuthenticatedServer(λ: suspend (HttpClient) -> T): T {
            return runBlocking {
                start().use { λ(it.restClient()) }
            }
        }
    }

    class TestServerRuntime(
        build: Route.() -> Unit,
        private val httpPort: Int
    ) : AutoCloseable {
        private val server = createEmbeddedServer(build, httpPort)

        companion object {
            private fun createEmbeddedServer(build: Route.() -> Unit, httpPort: Int) =
                embeddedServer(Netty, port = httpPort) {
                    install(ContentNegotiation) {
                        register(
                            ContentType.Application.Json,
                            JacksonConverter(objectMapper)
                        )
                    }
                    val jwkProvider = jwtStub.getJwkProviderMock()
                    val azureConfig =
                        AzureAdAppConfig(
                            clientId = clientId,
                            issuer = issuer,
                            jwkProvider = jwkProvider
                        )
                    azureAdAppAuthentication(azureConfig)
                    routing {
                        authenticate("oidc", build = build)
                    }
                }
        }

        init {
            server.start(wait = false)
        }

        override fun close() {
            server.stop(0, 0)
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

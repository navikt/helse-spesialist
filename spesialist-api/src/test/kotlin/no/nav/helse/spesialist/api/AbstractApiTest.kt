package no.nav.helse.spesialist.api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
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
        internal val jwtStub = JwtStub()
        private val requiredGroup: UUID = UUID.randomUUID()
        internal const val clientId = "client_id"
        internal const val issuer = "https://jwt-provider-domain"
        private const val epostadresse = "sara.saksbehandler@nav.no"

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

    }

    class TestServerRuntime(
        build: Route.() -> Unit,
        private val httpPort: Int
    ) : AutoCloseable {
        private val server = createEmbeddedServer(build, httpPort)

        companion object {
            private fun createEmbeddedServer(build: Route.() -> Unit, httpPort: Int) =
                embeddedServer(CIO, port = httpPort) {
                    install(ContentNegotiationServer) {
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
                install(ContentNegotiation) {
                    register(
                        ContentType.Application.Json,
                        JacksonConverter(objectMapper)
                    )
                }
            }
        }
    }
}

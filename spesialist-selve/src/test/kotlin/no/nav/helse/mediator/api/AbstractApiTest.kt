package no.nav.helse.mediator.api

import io.ktor.server.application.Application
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
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import java.net.ServerSocket
import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.api.AbstractApiTest.TestServerRuntime.Companion.routeToBeAuthenticated
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.AzureAdAppConfig
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

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
        internal const val clientId = "client_id"
        private const val epostadresse = "sara.saksbehandler@nav.no"
        internal const val issuer = "https://jwt-provider-domain"
        internal val jwtStub = JwtStub()
        private val azureConfig = AzureConfig(
            clientId = clientId,
            issuer = issuer,
            jwkProvider = jwtStub.getJwkProviderMock(),
            tokenEndpoint = "",
        )
        internal val azureAdAppConfig = AzureAdAppConfig(
            azureConfig = azureConfig,
        )

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

        fun HttpRequestBuilder.authentication(oid: UUID, epost: String = epostadresse, navn:String, ident:String, group: String? = null) {
            header(
                "Authorization",
                "Bearer ${
                    jwtStub.getToken(
                        groups = listOfNotNull(requiredGroup.toString(), group),
                        oid = oid.toString(),
                        epostadresse = epost,
                        clientId = clientId,
                        issuer = issuer,
                        navn = navn,
                        navIdent = ident
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
        private val httpPort: Int,
    ) : AutoCloseable {
        private val server = createEmbeddedServer(build, httpPort)

        companion object {
            // Må ta vare på den her, fordi det ikke går an å sende parametere til module-funksjonen
            internal var routeToBeAuthenticated: Route.() -> Unit = { }

            private fun createEmbeddedServer(build: Route.() -> Unit, httpPort: Int): CIOApplicationEngine {
                routeToBeAuthenticated = build
                return embeddedServer(CIO, port = httpPort, module = Application::module)
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

internal fun Application.module() {
    install(ContentNegotiationServer) {
        register(
            ContentType.Application.Json,
            JacksonConverter(objectMapper)
        )
    }
    val azureAdAppConfig = AzureAdAppConfig(
        azureConfig = AzureConfig(
            clientId = AbstractApiTest.clientId,
            issuer = AbstractApiTest.issuer,
            jwkProvider = AbstractApiTest.jwtStub.getJwkProviderMock(),
            tokenEndpoint = "",
        ),
    )

    azureAdAppAuthentication(azureAdAppConfig)
    routing { authenticate("oidc", build = routeToBeAuthenticated) }
}

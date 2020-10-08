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
    private lateinit var server: ApplicationEngine
    private val httpPort = ServerSocket(0).use { it.localPort }
    protected val requiredGroup = "required_group"
    protected val clientId = "client_id"
    protected val epostadresse = "sara.saksbehandler@nav.no";
    protected val issuer = "https://jwt-provider-domain"
    protected val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort

        }
        expectSuccess = false
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
    }
    internal val jwtStub = JwtStub()

    protected fun HttpRequestBuilder.authentication(oid: UUID) {
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

    protected fun setupServer(block: Route.() -> Unit) {
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
                    block()
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
}

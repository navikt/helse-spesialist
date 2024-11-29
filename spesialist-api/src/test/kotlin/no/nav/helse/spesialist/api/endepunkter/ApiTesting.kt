package no.nav.helse.spesialist.api.endepunkter

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplicationBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.objectMapper
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

internal class ApiTesting(
    private val jwtStub: JwtStub = JwtStub(),
    private val build: Route.() -> Unit,
) {
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"

    private fun TestApplicationBuilder.setUpApplication() {
        install(ContentNegotiationServer) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        val azureConfig =
            AzureConfig(
                clientId = clientId,
                issuerlUrl = issuer,
                jwkProvider = jwtStub.getJwkProviderMock(),
                tokenEndpoint = "",
            )
        application {
            azureAdAppAuthentication(azureConfig, Environment())
        }
        routing {
            authenticate("oidc", build = build)
        }
    }

    private fun ApplicationTestBuilder.httpClient() =
        createClient {
            expectSuccess = false
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    internal fun <T> spesialistApi(block: suspend ApplicationTestBuilder.(HttpClient) -> T): T {
        var response: T? = null
        testApplication {
            setUpApplication()
            response = block(httpClient())
        }
        return response!!
    }
}

package no.nav.helse.spesialist.api.endepunkter

import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.objectMapper
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

internal class ApiTesting(
    private val jwtStub: JwtStub = JwtStub(),
    private val applicationBuilder: ApplicationTestBuilder.() -> Unit,
    private val routeBuilder: Route.() -> Unit,
) {
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"

    private fun ApplicationTestBuilder.setUpApplication() {
        install(ServerContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        application {
            azureAdAppAuthentication(jwtStub.getJwkProviderMock(), issuer, clientId)
        }
        applicationBuilder(this)
        routing {
            authenticate("oidc", build = routeBuilder)
        }
    }

    private fun ApplicationTestBuilder.httpClient() =
        createClient {
            expectSuccess = false
            install(ClientContentNegotiation) {
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

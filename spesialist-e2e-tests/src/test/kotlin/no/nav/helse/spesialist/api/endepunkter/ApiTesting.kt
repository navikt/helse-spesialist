package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class ApiTesting(
    private val jwtStub: JwtStub = JwtStub(),
    private val applicationBuilder: ApplicationTestBuilder.() -> Unit,
    private val routeBuilder: Route.() -> Unit,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
) {
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"

    private fun ApplicationTestBuilder.setUpApplication() {
        install(ServerContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        application {
            azureAdAppAuthentication(
                jwkProvider = jwtStub.getJwkProviderMock(),
                issuerUrl = issuer,
                clientId = clientId,
                tilgangsgruppeUuider = tilgangsgruppeUuider,
                tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller
            )
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

    fun <T> spesialistApi(block: suspend ApplicationTestBuilder.() -> T): T {
        var response: T? = null
        testApplication {
            setUpApplication()
            client = httpClient()
            response = block()
        }
        return response!!
    }
}

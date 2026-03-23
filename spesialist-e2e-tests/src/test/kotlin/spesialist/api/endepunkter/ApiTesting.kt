package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.auth.configureJwtAuthentication
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class ApiTesting(
    private val jwtStub: JwtStub = JwtStub(),
    private val applicationBuilder: ApplicationTestBuilder.() -> Unit,
    private val routeBuilder: Route.() -> Unit,
    private val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    private val tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
) {
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"

    private fun ApplicationTestBuilder.setUpApplication() {
        install(ServerContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        application {
            authentication {
                this.jwt("oidc") {
                    configureJwtAuthentication(
                        jwkProvider = jwtStub.getJwkProviderMock(),
                        issuerUrl = issuer,
                        clientId = clientId,
                        tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
                        tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
                    )
                }
            }
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

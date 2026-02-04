package no.nav.helse.spesialist.api.testfixtures

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

class ApiModuleIntegrationTestFixture(
    private val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start),
    private val tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
    private val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
) {
    val token: String =
        mockOAuth2Server
            .issueToken(
                issuerId = ISSUER_ID,
                audience = CLIENT_ID,
                claims =
                    mapOf(
                        "preferred_username" to "saksbehandler@nav.no",
                        "oid" to "${UUID.randomUUID()}",
                        "name" to "En Saksbehandler",
                        "NAVident" to "X123456",
                    ),
            ).serialize()

    fun token(
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId = ISSUER_ID,
                audience = CLIENT_ID,
                subject = saksbehandler.id.value.toString(),
                claims =
                    mapOf(
                        "NAVident" to saksbehandler.ident.value,
                        "preferred_username" to saksbehandler.epost,
                        "oid" to saksbehandler.id.value.toString(),
                        "name" to saksbehandler.navn,
                        "groups" to (tilgangsgrupperTilTilganger.uuiderFor(tilganger) + tilgangsgrupperTilBrukerroller.uuiderFor(brukerroller)).map { it.toString() },
                    ),
            ).serialize()

    val apiModuleConfiguration =
        ApiModule.Configuration(
            clientId = CLIENT_ID,
            issuerUrl = mockOAuth2Server.issuerUrl(ISSUER_ID).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(ISSUER_ID).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(ISSUER_ID).toString(),
            eksponerOpenApi = true,
            versjonAvKode = "0.0.0",
        )

    fun addAdditionalRoutings(application: Application) {
        with(application) {
            routing {
                get("/local-token") {
                    return@get call.respond<String>(message = token)
                }
                get("playground") {
                    call.respondText(buildPlaygroundHtml(), ContentType.Text.Html)
                }
            }
        }
    }

    private fun buildPlaygroundHtml() =
        Application::class.java.classLoader
            .getResource("graphql-playground.html")
            ?.readText()
            ?.replace("\${graphQLEndpoint}", "graphql")
            ?.replace("\${subscriptionsEndpoint}", "subscriptions")
            ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")

    companion object {
        private const val CLIENT_ID = "en-client-id"
        private const val ISSUER_ID = "LocalTestIssuer"
    }
}

fun TilgangsgrupperTilBrukerroller.uuiderFor(brukerroller: Set<Brukerrolle>): List<UUID> {
    val uuider = mutableListOf<UUID>()
    if (Brukerrolle.EgenAnsatt in brukerroller) {
        uuider.addAll(egenAnsatt)
    }
    if (Brukerrolle.Kode7 in brukerroller) {
        uuider.addAll(kode7)
    }
    if (Brukerrolle.Beslutter in brukerroller) {
        uuider.addAll(beslutter)
    }
    if (Brukerrolle.SelvstendigNæringsdrivendeBeta in brukerroller) {
        uuider.addAll(næringsdrivendeBeta)
    }
    if (Brukerrolle.Stikkprøve in brukerroller) {
        uuider.addAll(stikkprøve)
    }
    if (Brukerrolle.Feilsøking in brukerroller) {
        uuider.addAll(utvikler)
    }
    return uuider
}

fun TilgangsgrupperTilTilganger.uuiderFor(tilganger: Set<Tilgang>): List<UUID> {
    val uuider = mutableListOf<UUID>()
    if (Tilgang.Les in tilganger) {
        uuider.addAll(lesetilgang)
    }
    if (Tilgang.Skriv in tilganger) {
        uuider.addAll(skrivetilgang)
    }
    return uuider
}

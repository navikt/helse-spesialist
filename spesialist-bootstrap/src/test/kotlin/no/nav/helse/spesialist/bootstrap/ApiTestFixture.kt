package no.nav.helse.spesialist.bootstrap

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helse.spesialist.api.ApiModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

object ApiTestFixture {
    private val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
    private const val CLIENT_ID = "en-client-id"
    private const val ISSUER_ID = "LocalTestIssuer"
    val token: String =
        mockOAuth2Server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "preferred_username" to "saksbehandler@nav.no",
                    "oid" to "${UUID.randomUUID()}",
                    "name" to "En Saksbehandler",
                    "NAVident" to "X123456",
                ),
        ).serialize().also {
            println("OAuth2-token:")
            println(it)
        }

    val apiModuleConfiguration = ApiModule.Configuration(
        clientId = CLIENT_ID,
        issuerUrl = mockOAuth2Server.issuerUrl(ISSUER_ID).toString(),
        jwkProviderUri = mockOAuth2Server.jwksUrl(ISSUER_ID).toString(),
        tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(ISSUER_ID).toString(),
    )

    fun addAdditionalRoutings(application: Application) {
        with (application) {
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

    private fun buildPlaygroundHtml() = Application::class.java.classLoader
        .getResource("graphql-playground.html")
        ?.readText()
        ?.replace("\${graphQLEndpoint}", "graphql")
        ?.replace("\${subscriptionsEndpoint}", "subscriptions")
        ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")
}

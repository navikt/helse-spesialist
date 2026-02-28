package no.nav.helse.spesialist.client.entraid.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.KeyProvider
import java.util.UUID

class ClientEntraIDModuleIntegrationTestFixture(
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start),
    val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller = TilgangsgrupperTilBrukerroller(
        næringsdrivendeBeta = (1..2).map { UUID.randomUUID() },
        beslutter = (1..2).map { UUID.randomUUID() },
        egenAnsatt = (1..2).map { UUID.randomUUID() },
        kode7 = (1..2).map { UUID.randomUUID() },
        stikkprøve = (1..2).map { UUID.randomUUID() },
        utvikler = (1..2).map { UUID.randomUUID() }
    )
) {
    val msGraphWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start
        ).also {
            it.stubFor(
                post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                    okJson("""{ "value": [] }""")
                )
            )
        }

    val oboTokenWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start
        ).also {
            it.stubFor(
                post(urlPathEqualTo("/")).willReturn(
                    okJson("""{ "access_token": "obo-test-token", "expires_in": 3599, "token_type": "Bearer" }""")
                )
            )
        }

    val moduleConfiguration = ClientEntraIDModule.Configuration(
        clientId = "123abc",
        tokenEndpoint = mockOAuth2Server.tokenEndpointUrl("default").toString(),
        privateJwk = KeyProvider().signingKey("blabla").toJSONString().also { println("JWK: $it") },
        msGraphUrl = msGraphWireMockServer.baseUrl(),
        oboTokenEndpoint = "${oboTokenWireMockServer.baseUrl()}/",
    )

    val module = ClientEntraIDModule(
        configuration = moduleConfiguration,
        tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
    )
}

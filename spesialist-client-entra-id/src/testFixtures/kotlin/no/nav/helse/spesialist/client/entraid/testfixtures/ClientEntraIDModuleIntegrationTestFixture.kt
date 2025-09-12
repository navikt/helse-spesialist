package no.nav.helse.spesialist.client.entraid.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.KeyProvider

class ClientEntraIDModuleIntegrationTestFixture(
    private val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
) {
    private val msGraphWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start
        ).also {
            it.stubFor(
                post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                    okJson("""{ "value": [] }""")
                )
            )
        }

    val entraIDAccessTokenGeneratorConfiguration = ClientEntraIDModule.Configuration(
        clientId = "123abc",
        tokenEndpoint = mockOAuth2Server.tokenEndpointUrl("default").toString(),
        privateJwk = KeyProvider().signingKey("blabla").toJSONString().also { println("JWK: $it") },
        msGraphUrl = msGraphWireMockServer.baseUrl(),
    )
}

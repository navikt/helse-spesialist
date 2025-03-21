package no.nav.helse.spesialist.client.entraid.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule

object ClientEntraIDModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    )

    val entraIDAccessTokenGeneratorConfiguration = ClientEntraIDModule.Configuration(
        clientId = "123abc",
        tokenEndpoint = wireMockServer.baseUrl(),
        privateJwk = "",
    )
}
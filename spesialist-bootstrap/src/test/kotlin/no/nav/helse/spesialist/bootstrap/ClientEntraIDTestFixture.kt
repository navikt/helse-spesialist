package no.nav.helse.spesialist.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule

object ClientEntraIDTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start)

    val entraIDAccessTokenGeneratorConfiguration = ClientEntraIDModule.Configuration(
        clientId = "123abc",
        tokenEndpoint = wireMockServer.baseUrl(),
        privateJwk = "",
    )
}

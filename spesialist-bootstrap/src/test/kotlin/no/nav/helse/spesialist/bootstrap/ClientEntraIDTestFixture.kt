package no.nav.helse.spesialist.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator

object ClientEntraIDTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start)

    val entraIDAccessTokenGeneratorConfiguration = EntraIDAccessTokenGenerator.Configuration(
        clientId = "123abc",
        tokenEndpoint = wireMockServer.baseUrl(),
        privateJwk = "",
    )
}

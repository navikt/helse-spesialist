package no.nav.helse.spesialist.client.spleis.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import java.net.URI

object ClientSpleisModuleIntegrationTestFixture {
    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    )

    val moduleConfiguration = ClientSpleisModule.Configuration(
        spleisUrl = URI.create(wireMockServer.baseUrl()),
        spleisClientId = "local-app",
        loggRespons = true,
    )
}

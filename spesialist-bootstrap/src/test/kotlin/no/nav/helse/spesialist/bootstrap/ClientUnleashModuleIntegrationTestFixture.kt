package no.nav.helse.spesialist.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

object ClientUnleashModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start)

    val moduleConfiguration = ClientUnleashModule.Configuration(
        apiKey = "123abc",
        apiUrl = wireMockServer.baseUrl(),
        apiEnv = "local-app"
    )
}

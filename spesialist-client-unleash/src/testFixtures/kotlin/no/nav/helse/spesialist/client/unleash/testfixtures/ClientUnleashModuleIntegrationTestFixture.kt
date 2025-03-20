package no.nav.helse.spesialist.client.unleash.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.unleash.ClientUnleashModule

object ClientUnleashModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    )

    val moduleConfiguration = ClientUnleashModule.Configuration(
        apiKey = "123abc",
        apiUrl = wireMockServer.baseUrl(),
        apiEnv = "local-app"
    )
}
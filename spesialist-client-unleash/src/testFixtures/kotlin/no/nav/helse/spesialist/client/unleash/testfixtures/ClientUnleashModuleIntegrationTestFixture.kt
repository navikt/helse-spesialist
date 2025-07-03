package no.nav.helse.spesialist.client.unleash.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.unleash.ClientUnleashModule

object ClientUnleashModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    ).also { it.stubFor(any(anyUrl()).willReturn(notFound())) }

    val moduleConfiguration = ClientUnleashModule.Configuration(
        apiKey = "123abc",
        apiUrl = wireMockServer.baseUrl(),
        apiEnv = "local-app"
    )
}

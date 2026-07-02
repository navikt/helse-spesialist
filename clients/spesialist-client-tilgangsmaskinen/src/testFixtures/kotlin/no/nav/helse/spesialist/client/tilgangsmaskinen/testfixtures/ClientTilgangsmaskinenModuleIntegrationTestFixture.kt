package no.nav.helse.spesialist.client.tilgangsmaskinen.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.noContent
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.tilgangsmaskinen.ClientTilgangsmaskinenModule

object ClientTilgangsmaskinenModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(WireMockServer::start).also {
            it.stubFor(post("/api/v1/komplett").willReturn(noContent()))
            it.stubFor(post("/api/v1/kjerne").willReturn(noContent()))
        }

    val moduleConfiguration =
        ClientTilgangsmaskinenModule.Configuration(
            scope = "local-app",
            baseUrl = wireMockServer.baseUrl(),
        )
}

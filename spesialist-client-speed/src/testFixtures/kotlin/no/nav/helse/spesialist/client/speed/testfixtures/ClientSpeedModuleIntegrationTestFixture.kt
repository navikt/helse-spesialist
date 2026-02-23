package no.nav.helse.spesialist.client.speed.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.speed.ClientSpeedModule

object ClientSpeedModuleIntegrationTestFixture {
    val wireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start,
        ).also { server ->
            server.stubFor(
                post(urlEqualTo("/api/historiske_identer")).willReturn(
                    okJson(
                        """{ "fødselsnumre": [], "kilde": "PDL" }""",
                    ),
                ),
            )
        }

    val moduleConfiguration =
        ClientSpeedModule.Configuration(
            apiUrl = wireMockServer.baseUrl(),
            scope = "local-app",
        )
}

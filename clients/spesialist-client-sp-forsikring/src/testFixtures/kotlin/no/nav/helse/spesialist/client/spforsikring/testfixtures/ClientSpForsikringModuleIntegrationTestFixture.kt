package no.nav.helse.spesialist.client.spforsikring.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.spforsikring.ClientSpForsikringModule

object ClientSpForsikringModuleIntegrationTestFixture {
    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    ).also {
        it.stubFor(
            get("/behandling/{spleisBehandlingId}/forsikring").willReturn(
                okJson(
                    """
                        {
                        "grad": 100,
                        "gjelderFraDag": 17
                        }
                    """.trimIndent()
                )
            )
        )
    }

    val moduleConfiguration = ClientSpForsikringModule.Configuration(
        apiUrl = wireMockServer.baseUrl(),
        scope = "local-app"
    )
}

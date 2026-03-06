package no.nav.helse.spesialist.client.sparkel.norg.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.sparkel.norg.ClientSparkelNorgModule

object ClientSparkelNorgModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start,
        ).also {
            it.stubFor(
                post("/api/behandlende-enhet").willReturn(
                    okJson(
                        """
                        {
                          "enhetNr": "0101",
                          "navn": "Nav Halden",
                          "type": "LOKAL"
                        }
                        """.trimIndent(),
                    ),
                ),
            )
        }

    val moduleConfiguration =
        ClientSparkelNorgModule.Configuration(
            apiUrl = wireMockServer.baseUrl(),
            scope = "local-app",
        )
}

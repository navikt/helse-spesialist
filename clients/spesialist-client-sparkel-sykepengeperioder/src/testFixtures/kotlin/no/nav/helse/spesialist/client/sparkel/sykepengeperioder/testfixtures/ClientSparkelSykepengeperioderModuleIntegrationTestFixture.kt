package no.nav.helse.spesialist.client.sparkel.sykepengeperioder.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.sparkel.sykepengeperioder.ClientSparkelSykepengeperioderModule

object ClientSparkelSykepengeperioderModuleIntegrationTestFixture {
    private val wireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start,
        ).also {
            it.stubFor(
                post("/").willReturn(
                    okJson(
                        """
                        {
                          "utbetaltePerioder": []
                        }
                        """.trimIndent(),
                    ),
                ),
            )
        }

    val moduleConfiguration =
        ClientSparkelSykepengeperioderModule.Configuration(
            apiUrl = wireMockServer.baseUrl(),
            scope = "local-app",
        )
}

package no.nav.helse.spesialist.client.krr.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.krr.ClientKrrModule

object ClientKRRModuleIntegationTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    ).also {
        it.stubFor(
            post("/rest/v1/personer").willReturn(
                okJson(
                    """
                        {
                          "personer": {
                            "{{jsonPath request.body '$.personidenter[0]'}}": {
                              "kanVarsles": true,
                              "reservert": false
                            }
                          },
                          "feil": {}
                        }
                        """.trimIndent()
                ).withTransformers("response-template")
            )
        )
    }

    val moduleConfiguration = ClientKrrModule.Configuration(
        apiUrl = wireMockServer.baseUrl(),
        scope = "local-app",
    )
}

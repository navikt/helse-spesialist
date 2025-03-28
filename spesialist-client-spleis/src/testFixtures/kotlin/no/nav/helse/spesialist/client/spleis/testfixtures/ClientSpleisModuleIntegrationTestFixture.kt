package no.nav.helse.spesialist.client.spleis.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import org.intellij.lang.annotations.Language
import java.net.URI
import java.util.UUID

object ClientSpleisModuleIntegrationTestFixture {
    @Language("JSON")
    val data = """
        {
            "data": {
                "person": {
                    "aktorId": "",
                    "arbeidsgivere": [
                      {
                        "organisasjonsnummer": "123456789",
                        "ghostPerioder": [],
                        "nyesteInntektsforholdPerioder": [],
                        "generasjoner": [
                          {
                            "id": "${UUID.randomUUID()}",
                            "perioder": [
                               {
                                  "varsel": 
                               }
                            ]
                          }
                        ]     
                      }
                    ],
                    "dodsdato": null,
                    "fodselsnummer": "12345678901",
                    "versjon": 1,
                    "vilkarsgrunnlag": []
                }
            }
        }
        """.trimIndent()
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    ).apply {
        stubFor(post("/graphql")
            .willReturn(
                aResponse()
                    .withBody(
                        data
                    )
            ))
    }

    val moduleConfiguration = ClientSpleisModule.Configuration(
        spleisUrl = URI.create(wireMockServer.baseUrl()),
        spleisClientId = "local-app",
    )
}
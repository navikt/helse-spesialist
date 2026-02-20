package no.nav.helse.spesialist.client.spillkar.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.spillkar.ClientSpillkarModule
import java.util.UUID

object ClientSpillkarModuleIntegrationTestFixture {
    val wireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
            WireMockServer::start,
        ).also { server ->
            val samlingId = UUID.randomUUID()
            server.stubFor(
                post(urlEqualTo("/vurderte-inngangsvilkår/alle")).willReturn(
                    okJson(
                        """
                        {
                          "samlingAvVurderteInngangsvilkår": [
                            {
                              "samlingAvVurderteInngangsvilkårId": "$samlingId",
                              "versjon": 1,
                              "skjæringstidspunkt": "2021-01-01",
                              "vurderteInngangsvilkår": []
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )
            server.stubFor(
                post(urlEqualTo("/vurderte-inngangsvilkår/manuelle-vurderinger"))
                    .willReturn(com.github.tomakehurst.wiremock.client.WireMock.noContent()),
            )
        }

    val moduleConfiguration =
        ClientSpillkarModule.Configuration(
            apiUrl = wireMockServer.baseUrl(),
            scope = "local-app",
        )
}

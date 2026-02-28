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
                post(urlEqualTo("/api/alle_identer")).willReturn(
                    okJson(
                        """{ "identer": [], "kilde": "PDL" }""",
                    ),
                ),
            )
            server.stubFor(
                post(urlEqualTo("/api/person")).willReturn(
                    okJson(
                        """{ "fødselsdato": "1990-01-01", "dødsdato": null, "fornavn": "Test", "mellomnavn": null, "etternavn": "Testesen", "adressebeskyttelse": "UGRADERT", "kjønn": "UKJENT", "kilde": "PDL" }""",
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

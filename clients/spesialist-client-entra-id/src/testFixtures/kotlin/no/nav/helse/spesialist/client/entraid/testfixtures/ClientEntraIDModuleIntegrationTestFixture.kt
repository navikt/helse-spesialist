package no.nav.helse.spesialist.client.entraid.testfixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import java.util.UUID

class ClientEntraIDModuleIntegrationTestFixture(
    val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller =
        TilgangsgrupperTilBrukerroller(
            næringsdrivendeBeta = (1..2).map { UUID.randomUUID() },
            beslutter = (1..2).map { UUID.randomUUID() },
            egenAnsatt = (1..2).map { UUID.randomUUID() },
            kode7 = (1..2).map { UUID.randomUUID() },
            stikkprøve = (1..2).map { UUID.randomUUID() },
            utvikler = (1..2).map { UUID.randomUUID() },
            dialogmelding = (1..2).map { UUID.randomUUID() },
        ),
) {
    val msGraphWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort())
            .also(
                WireMockServer::start,
            ).also {
                it.stubFor(
                    post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                        okJson("""{ "value": [] }"""),
                    ),
                )
            }

    val texasWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.options().dynamicPort())
            .also(
                WireMockServer::start,
            ).also {
                it.stubFor(
                    post(urlPathEqualTo("/token")).willReturn(
                        okJson("""{ "access_token": "stub-access-token", "expires_in": 3599, "token_type": "Bearer" }"""),
                    ),
                )
                it.stubFor(
                    post(urlPathEqualTo("/token/exchange")).willReturn(
                        okJson("""{ "access_token": "stub-obo-token", "expires_in": 3599, "token_type": "Bearer" }"""),
                    ),
                )
            }

    val moduleConfiguration =
        ClientEntraIDModule.Configuration(
            tokenEndpoint = "${texasWireMockServer.baseUrl()}/token",
            tokenExchangeEndpoint = "${texasWireMockServer.baseUrl()}/token/exchange",
            msGraphUrl = msGraphWireMockServer.baseUrl(),
        )

    val module =
        ClientEntraIDModule(
            configuration = moduleConfiguration,
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
        )
}

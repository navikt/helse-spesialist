package no.nav.helse.spesialist.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter

object ClientKRRTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start
    )

    val moduleConfiguration = KRRClientReservasjonshenter.Configuration(
        KRRClientReservasjonshenter.Configuration.Client(
            apiUrl = wireMockServer.baseUrl(),
            scope = "local-app",
        )
    )
}

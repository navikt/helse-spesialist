package no.nav.helse.spesialist.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.helse.spesialist.client.spleis.SpleisClient
import java.net.URI

object ClientSpleisTestFixture {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort()).also(
        WireMockServer::start)

    val moduleConfiguration = SpleisClient.Configuration(
        spleisUrl = URI.create(wireMockServer.baseUrl()),
        spleisClientId = "local-app",
    )
}

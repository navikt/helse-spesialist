package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.bootstrap.Environment
import no.nav.helse.bootstrap.SpesialistApp
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.azureAdClient
import no.nav.helse.spesialist.api.bootstrap.httpClient
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spesialist.api.reservasjon.KRRClient
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import java.net.URI

fun main() {
    val applicationBuilder = RapidApp(System.getenv())
    applicationBuilder.start()
}

internal class RapidApp(env: Map<String, String>) {
    private lateinit var rapidsConnection: RapidsConnection
    private val azureConfig =
        AzureConfig(
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
            jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
            tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        )
    private val accessTokenClient =
        AccessTokenClient(
            httpClient = azureAdClient(),
            azureConfig = azureConfig,
            privateJwk = env.getValue("AZURE_APP_JWK"),
        )
    private val snapshotClient =
        SnapshotClient(
            httpClient = httpClient(120_000, 1_000, 40_000),
            accessTokenClient = accessTokenClient,
            spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
            spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
        )
    private val reservasjonClient =
        KRRClient(
            httpClient = httpClient(5_000, 5_000, 5_000),
            accessTokenClient = accessTokenClient,
            apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
            scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
        )

    private val msGraphClient = MsGraphClient(httpClient(120_000, 1_000, 40_000), accessTokenClient)

    private val tilgangsgrupper = SpeilTilgangsgrupper(System.getenv())
    private val spesialistApp =
        SpesialistApp(
            env = Environment(),
            gruppekontroll = msGraphClient,
            snapshotClient = snapshotClient,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonClient = reservasjonClient,
            versjonAvKode = versjonAvKode(env),
        ) {
            rapidsConnection
        }

    private fun versjonAvKode(env: Map<String, String>): String {
        return env["NAIS_APP_IMAGE"] ?: throw IllegalArgumentException("NAIS_APP_IMAGE env variable is missing")
    }

    init {
        val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        Metrics.globalRegistry.add(prometheusMeterRegistry)

        rapidsConnection =
            RapidApplication.create(
                env = env,
                meterRegistry = prometheusMeterRegistry,
                builder = {
                    withKtorModule(spesialistApp::ktorApp)
                },
            )
    }

    fun start() = spesialistApp.start()
}

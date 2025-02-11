package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.bootstrap.EnvironmentImpl
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.httpClient
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import java.net.URI

fun main() {
    RapidApp(System.getenv())
}

internal class RapidApp(env: Map<String, String>) {
    private val rapidsConnection: RapidsConnection
    private val unleashFeatureToggles =
        UnleashFeatureToggles(
            env = env,
        )
    private val azureConfig =
        AzureConfig(
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
            jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
            tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        )
    private val accessTokenGenerator =
        EntraIDAccessTokenGenerator(
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            privateJwk = env.getValue("AZURE_APP_JWK"),
        )
    private val snapshotClient =
        SnapshotClient(
            httpClient = httpClient(120_000, 1_000, 40_000),
            accessTokenGenerator = accessTokenGenerator,
            spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
            spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
        )
    private val reservasjonClient =
        KRRClientReservasjonshenter(
            apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
            scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
            accessTokenGenerator = accessTokenGenerator,
        )

    private val tilgangsgrupper = SpeilTilgangsgrupper(System.getenv())
    private val spesialistApp =
        SpesialistApp(
            env = EnvironmentImpl(),
            gruppekontroll = MsGraphGruppekontroll(accessTokenGenerator),
            snapshotClient = snapshotClient,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonshenter = reservasjonClient,
            versjonAvKode = versjonAvKode(env),
            featureToggles = unleashFeatureToggles,
        )

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
        spesialistApp.start(rapidsConnection)
    }
}

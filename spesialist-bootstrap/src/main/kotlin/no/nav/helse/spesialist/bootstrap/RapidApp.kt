package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.db.bootstrap.DBModule
import org.slf4j.LoggerFactory
import java.net.URI

fun main() {
    RapidApp(System.getenv())
}

internal class RapidApp(env: Map<String, String>) {
    private val logger = LoggerFactory.getLogger(RapidApp::class.java)
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
    private val snapshothenter =
        SpleisClientSnapshothenter(
            SpleisClient(
                accessTokenGenerator = accessTokenGenerator,
                spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
                spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
            ),
        )
    val environment = EnvironmentImpl()
    private val reservasjonshenter =
        if (environment.brukDummyForKRR) {
            logger.info("Bruker nulloperasjonsversjon av reservasjonshenter")
            Reservasjonshenter { null }
        } else {
            KRRClientReservasjonshenter(
                apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
                scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
                accessTokenGenerator = accessTokenGenerator,
            )
        }

    private val tilgangsgrupper = SpeilTilgangsgrupper(System.getenv())
    private val spesialistApp =
        SpesialistApp(
            env = environment,
            gruppekontroll = MsGraphGruppekontroll(accessTokenGenerator),
            snapshothenter = snapshothenter,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonshenter = reservasjonshenter,
            versjonAvKode = versjonAvKode(env),
            featureToggles = unleashFeatureToggles,
            dbModuleConfiguration =
                DBModule.Configuration(
                    jdbcUrl =
                        "jdbc:postgresql://" +
                            env.getRequired("DATABASE_HOST") +
                            ":" +
                            env.getRequired("DATABASE_PORT") +
                            "/" +
                            env.getRequired("DATABASE_DATABASE"),
                    username = env.getRequired("DATABASE_USERNAME"),
                    password = env.getRequired("DATABASE_PASSWORD"),
                ),
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
                    withKtorModule(spesialistApp::konfigurerKtorApp)
                },
            )
        spesialistApp.start(rapidsConnection)
    }
}

private fun Map<String, String>.getRequired(name: String) = this[name] ?: error("$name må settes")

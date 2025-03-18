package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.bootstrap.Environment
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.automatisering.StikkprøverImpl
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.db.bootstrap.DBModule
import org.slf4j.LoggerFactory

fun main() {
    RapidApp(System.getenv())
}

internal class RapidApp(env: Map<String, String>) {
    data class Configuration(
        val azureConfig: AzureConfig,
        val accessTokenGenerator: EntraIDAccessTokenGenerator,
        val spleisClientConfig: SpleisClient.Configuration,
        val krrConfig: KRRClientReservasjonshenter.Configuration,
        val dbConfig: DBModule.Configuration,
        val unleashFeatureToggles: UnleashFeatureToggles.Configuration,
        val versjonAvKode: String,
        val tilgangsgrupper: Tilgangsgrupper,
        val environment: Environment,
        val stikkprøver: Stikkprøver,
    ) {
        companion object {
            fun fraEnv(env: Map<String, String>): Configuration {
                val accessTokenGenerator =
                    EntraIDAccessTokenGenerator(EntraIDAccessTokenGenerator.Configuration.fraEnv(env))
                return Configuration(
                    azureConfig = AzureConfig.fraEnv(env),
                    accessTokenGenerator = accessTokenGenerator,
                    spleisClientConfig = SpleisClient.Configuration.fraEnv(env),
                    krrConfig = KRRClientReservasjonshenter.Configuration.fraEnv(env),
                    dbConfig = DBModule.Configuration.fraEnv(env),
                    unleashFeatureToggles = UnleashFeatureToggles.Configuration.fraEnv(env),
                    versjonAvKode = env.getValue("NAIS_APP_IMAGE"),
                    tilgangsgrupper = SpeilTilgangsgrupper(env),
                    environment = EnvironmentImpl(env),
                    stikkprøver = StikkprøverImpl(env),
                )
            }
        }
    }

    private val logger = LoggerFactory.getLogger(RapidApp::class.java)
    private val rapidsConnection: RapidsConnection
    private val configuration = Configuration.fraEnv(env)

    private val reservasjonshenter =
        if (configuration.environment.brukDummyForKRR) {
            logger.info("Bruker nulloperasjonsversjon av reservasjonshenter")
            Reservasjonshenter { null }
        } else {
            KRRClientReservasjonshenter(
                configuration = configuration.krrConfig,
                accessTokenGenerator = configuration.accessTokenGenerator,
            )
        }

    private val spesialistApp =
        SpesialistApp(
            env = configuration.environment,
            gruppekontroll = MsGraphGruppekontroll(configuration.accessTokenGenerator),
            snapshothenter =
                SpleisClientSnapshothenter(
                    SpleisClient(
                        accessTokenGenerator = configuration.accessTokenGenerator,
                        configuration = configuration.spleisClientConfig,
                    ),
                ),
            azureConfig = configuration.azureConfig,
            tilgangsgrupper = configuration.tilgangsgrupper,
            reservasjonshenter = reservasjonshenter,
            versjonAvKode = configuration.versjonAvKode,
            featureToggles = UnleashFeatureToggles(configuration = configuration.unleashFeatureToggles),
            dbModuleConfiguration = configuration.dbConfig,
            stikkprøver = configuration.stikkprøver,
        )

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

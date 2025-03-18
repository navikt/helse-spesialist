package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import org.slf4j.LoggerFactory

fun main() {
    RapidApp(System.getenv())
}

internal class RapidApp(env: Map<String, String>) {
    private val logger = LoggerFactory.getLogger(RapidApp::class.java)
    private val rapidsConnection: RapidsConnection
    private val configuration = Configuration.fraEnv(env)
    private val accessTokenGenerator = EntraIDAccessTokenGenerator(configuration.accessTokenGeneratorConfig)

    private val reservasjonshenter =
        if (configuration.environmentToggles.brukDummyForKRR) {
            logger.info("Bruker nulloperasjonsversjon av reservasjonshenter")
            Reservasjonshenter { null }
        } else {
            KRRClientReservasjonshenter(
                configuration = configuration.krrConfig,
                accessTokenGenerator = accessTokenGenerator,
            )
        }

    private val spesialistApp =
        SpesialistApp(
            environmentToggles = configuration.environmentToggles,
            gruppekontroll = MsGraphGruppekontroll(accessTokenGenerator),
            snapshothenter =
                SpleisClientSnapshothenter(
                    SpleisClient(
                        accessTokenGenerator = accessTokenGenerator,
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

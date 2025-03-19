package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
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
    RapidApp.start(
        configuration = Configuration.fraEnv(System.getenv()),
        rapidsConnection =
            RapidApplication.create(
                env = System.getenv(),
                meterRegistry =
                    PrometheusMeterRegistry(
                        PrometheusConfig.DEFAULT,
                    ).also<PrometheusMeterRegistry>(Metrics.globalRegistry::add),
                builder = {
                    withKtorModule {
                        // Deferret fordi det er sirkulær avhengighet mellom RapidApplication og Ktor-oppsettet...
                        RapidApp.ktorSetupCallback(this)
                    }
                },
            ),
    )
}

object RapidApp {
    lateinit var ktorSetupCallback: Application.() -> Unit

    fun start(
        configuration: Configuration,
        rapidsConnection: RapidsConnection,
    ) {
        val accessTokenGenerator = EntraIDAccessTokenGenerator(configuration.accessTokenGeneratorConfig)

        val reservasjonshenter =
            if (configuration.environmentToggles.brukDummyForKRR) {
                LoggerFactory.getLogger(SpesialistApp::class.java)
                    .info("Bruker nulloperasjonsversjon av reservasjonshenter")
                Reservasjonshenter { null }
            } else {
                KRRClientReservasjonshenter(
                    configuration = configuration.krrConfig,
                    accessTokenGenerator = accessTokenGenerator,
                )
            }

        val spesialistApp =
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

        ktorSetupCallback = spesialistApp::konfigurerKtorApp

        spesialistApp.start(rapidsConnection)
    }
}

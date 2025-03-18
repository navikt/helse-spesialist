package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.bootstrap.Environment
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.automatisering.Stikkprøver
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
import java.net.URI
import kotlin.random.Random

fun main() {
    RapidApp(System.getenv())
}

internal class RapidApp(env: Map<String, String>) {
    data class Configuration(
        val azureConfig: AzureConfig,
        val accessTokenGenerator: EntraIDAccessTokenGenerator,
        val snapshothenter: SpleisClientSnapshothenter,
        val reservasjonshenter: Reservasjonshenter,
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
                    snapshothenter =
                        SpleisClientSnapshothenter(
                            SpleisClient(
                                accessTokenGenerator = accessTokenGenerator,
                                spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
                                spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
                            ),
                        ),
                    reservasjonshenter =
                        KRRClientReservasjonshenter(
                            apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
                            scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
                            accessTokenGenerator = accessTokenGenerator,
                        ),
                    dbConfig = DBModule.Configuration.fraEnv(env),
                    unleashFeatureToggles = UnleashFeatureToggles.Configuration.fraEnv(env),
                    versjonAvKode = versjonAvKode(env),
                    tilgangsgrupper = SpeilTilgangsgrupper(env),
                    environment = EnvironmentImpl(env),
                    stikkprøver =
                        object : Stikkprøver {
                            override fun utsFlereArbeidsgivereFørstegangsbehandling() =
                                plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FGB_DIVISOR"])

                            override fun utsFlereArbeidsgivereForlengelse() =
                                plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FORLENGELSE_DIVISOR"])

                            override fun utsEnArbeidsgiverFørstegangsbehandling() =
                                plukkTilManuell(
                                    env["STIKKPROEVER_UTS_EN_AG_FGB_DIVISOR"],
                                )

                            override fun utsEnArbeidsgiverForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FORLENGELSE_DIVISOR"])

                            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() =
                                plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FGB_DIVISOR"])

                            override fun fullRefusjonFlereArbeidsgivereForlengelse() =
                                plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FORLENGELSE_DIVISOR"])

                            override fun fullRefusjonEnArbeidsgiver() = plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_EN_AG_DIVISOR"])
                        },
                )
            }

            private val plukkTilManuell: PlukkTilManuell<String> = (
                {
                    it?.let {
                        val divisor = it.toInt()
                        require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
                        Random.nextInt(divisor) == 0
                    } == true
                }
            )

            private fun versjonAvKode(env: Map<String, String>): String {
                return env["NAIS_APP_IMAGE"] ?: throw IllegalArgumentException("NAIS_APP_IMAGE env variable is missing")
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
            configuration.reservasjonshenter
        }

    private val spesialistApp =
        SpesialistApp(
            env = configuration.environment,
            gruppekontroll = MsGraphGruppekontroll(configuration.accessTokenGenerator),
            snapshothenter = configuration.snapshothenter,
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

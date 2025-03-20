package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.helse.spesialist.client.krr.ClientKrrModule
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.db.FlywayMigrator
import no.nav.helse.spesialist.kafka.KafkaModule
import org.slf4j.LoggerFactory
import java.net.URI

fun main() {
    val env = System.getenv()
    val versjonAvKode = env.getValue("NAIS_APP_IMAGE")
    RapidApp.start(
        configuration =
            Configuration(
                apiModuleConfiguration =
                    ApiModule.Configuration(
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                        jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                    ),
                accessTokenGeneratorConfig =
                    ClientEntraIDModule.Configuration(
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                        privateJwk = env.getValue("AZURE_APP_JWK"),
                    ),
                spleisClientConfig =
                    ClientSpleisModule.Configuration(
                        spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
                        spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
                    ),
                krrConfig =
                    ClientKrrModule.Configuration(
                        if (env.getBoolean("BRUK_DUMMY_FOR_KONTAKT_OG_RESERVASJONSREGISTERET", false)) {
                            null
                        } else {
                            ClientKrrModule.Configuration.Client(
                                apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
                                scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
                            )
                        },
                    ),
                dbConfig =
                    DBModule.Configuration(
                        jdbcUrl =
                            "jdbc:postgresql://" +
                                env.getValue("DATABASE_HOST") +
                                ":" +
                                env.getValue("DATABASE_PORT") +
                                "/" +
                                env.getValue("DATABASE_DATABASE"),
                        username = env.getValue("DATABASE_USERNAME"),
                        password = env.getValue("DATABASE_PASSWORD"),
                    ),
                kafkaModuleConfiguration =
                    KafkaModule.Configuration(
                        versjonAvKode = versjonAvKode,
                        ignorerMeldingerForUkjentePersoner = env.getBoolean("IGNORER_MELDINGER_FOR_UKJENTE_PERSONER", false),
                    ),
                unleashFeatureToggles =
                    UnleashFeatureToggles.Configuration(
                        apiKey = env.getValue("UNLEASH_SERVER_API_TOKEN"),
                        apiUrl = env.getValue("UNLEASH_SERVER_API_URL"),
                        apiEnv = env.getValue("UNLEASH_SERVER_API_ENV"),
                    ),
                versjonAvKode = versjonAvKode,
                tilgangsgrupper = SpeilTilgangsgrupper(env),
                environmentToggles = EnvironmentTogglesImpl(env),
                stikkprøver = Stikkprøver.fraEnv(env),
            ),
        rapidsConnection =
            RapidApplication.create(
                env = env,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).also(Metrics.globalRegistry::add),
                builder = {
                    withKtorModule {
                        // Deferret fordi det er sirkulær avhengighet mellom RapidApplication og Ktor-oppsettet...
                        RapidApp.ktorSetupCallback(this)
                    }
                },
            ),
    )
}

private fun Map<String, String>.getBoolean(
    key: String,
    defaultValue: Boolean,
): Boolean = this[key]?.toBoolean() ?: defaultValue

object RapidApp {
    lateinit var ktorSetupCallback: Application.() -> Unit

    fun start(
        configuration: Configuration,
        rapidsConnection: RapidsConnection,
    ) {
        val dbModule = DBModule(configuration.dbConfig)
        val daos: Daos = dbModule.daos
        val sessionFactory: SessionFactory = dbModule.sessionFactory
        val flywayMigrator: FlywayMigrator = dbModule.flywayMigrator

        val clientEntraIdModule = ClientEntraIDModule(configuration.accessTokenGeneratorConfig)
        val accessTokenGenerator = clientEntraIdModule.accessTokenGenerator
        val gruppekontroll = clientEntraIdModule.gruppekontroll

        val featureToggles = UnleashFeatureToggles(configuration = configuration.unleashFeatureToggles)

        val versjonAvKode = configuration.versjonAvKode
        val kafkaModule =
            KafkaModule(
                configuration = configuration.kafkaModuleConfiguration,
                rapidsConnection = rapidsConnection,
            )

        val meldingPubliserer = kafkaModule.meldingPubliserer

        kafkaModule.setUpKafka(
            sessionFactory = sessionFactory,
            daos = daos,
            tilgangsgrupper = configuration.tilgangsgrupper,
            stikkprøver = configuration.stikkprøver,
            featureToggles = featureToggles,
            gruppekontroll = gruppekontroll,
        )

        val clientSpleisModule =
            ClientSpleisModule(
                configuration = configuration.spleisClientConfig,
                accessTokenGenerator = accessTokenGenerator,
            )
        val snapshothenter = clientSpleisModule.snapshothenter

        val clientKrrModule =
            ClientKrrModule(
                configuration = configuration.krrConfig,
                accessTokenGenerator = accessTokenGenerator,
            )
        val reservasjonshenter = clientKrrModule.reservasjonshenter

        ktorSetupCallback = {
            ApiModule(configuration.apiModuleConfiguration).setUpApi(
                daos = daos,
                tilgangsgrupper = configuration.tilgangsgrupper,
                meldingPubliserer = meldingPubliserer,
                gruppekontroll = gruppekontroll,
                sessionFactory = sessionFactory,
                versjonAvKode = versjonAvKode,
                environmentToggles = configuration.environmentToggles,
                featureToggles = featureToggles,
                snapshothenter = snapshothenter,
                reservasjonshenter = reservasjonshenter,
                application = this,
            )
        }

        rapidsConnection.register(
            object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    flywayMigrator.migrate()
                }
            },
        )

        rapidsConnection.start()
    }
}

private val logg = LoggerFactory.getLogger("SpesialistApp")

package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.db.FlywayMigrator
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.net.URI

fun main() {
    val env = System.getenv()
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
                    EntraIDAccessTokenGenerator.Configuration(
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                        privateJwk = env.getValue("AZURE_APP_JWK"),
                    ),
                spleisClientConfig =
                    SpleisClient.Configuration(
                        spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
                        spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
                    ),
                krrConfig =
                    KRRClientReservasjonshenter.Configuration(
                        if (env.getBoolean("BRUK_DUMMY_FOR_KONTAKT_OG_RESERVASJONSREGISTERET", false)) {
                            null
                        } else {
                            KRRClientReservasjonshenter.Configuration.Client(
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
                unleashFeatureToggles =
                    UnleashFeatureToggles.Configuration(
                        apiKey = env.getValue("UNLEASH_SERVER_API_TOKEN"),
                        apiUrl = env.getValue("UNLEASH_SERVER_API_URL"),
                        apiEnv = env.getValue("UNLEASH_SERVER_API_ENV"),
                    ),
                versjonAvKode = env.getValue("NAIS_APP_IMAGE"),
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

        val accessTokenGenerator = EntraIDAccessTokenGenerator(configuration.accessTokenGeneratorConfig)

        val featureToggles = UnleashFeatureToggles(configuration = configuration.unleashFeatureToggles)

        val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)

        RiverSetup(
            rapidsConnection,
            MeldingMediator(
                sessionFactory = sessionFactory,
                personDao = daos.personDao,
                commandContextDao = daos.commandContextDao,
                meldingDao = daos.meldingDao,
                meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
                kommandofabrikk =
                    Kommandofabrikk(
                        oppgaveService = {
                            OppgaveService(
                                oppgaveDao = daos.oppgaveDao,
                                reservasjonDao = daos.reservasjonDao,
                                meldingPubliserer = meldingPubliserer,
                                tilgangskontroll =
                                    TilgangskontrollørForReservasjon(
                                        MsGraphGruppekontroll(
                                            accessTokenGenerator,
                                        ),
                                        configuration.tilgangsgrupper,
                                    ),
                                tilgangsgrupper = configuration.tilgangsgrupper,
                                oppgaveRepository = daos.oppgaveRepository,
                            )
                        },
                        godkjenningMediator = GodkjenningMediator(daos.opptegnelseDao),
                        subsumsjonsmelderProvider = {
                            Subsumsjonsmelder(
                                configuration.versjonAvKode,
                                meldingPubliserer,
                            )
                        },
                        stikkprøver = configuration.stikkprøver,
                        featureToggles = featureToggles,
                    ),
                dokumentDao = daos.dokumentDao,
                varselRepository =
                    VarselRepository(
                        varselDao = daos.varselDao,
                        definisjonDao = daos.definisjonDao,
                    ),
                poisonPillDao = daos.poisonPillDao,
                environmentToggles = configuration.environmentToggles,
            ),
            daos.meldingDuplikatkontrollDao,
        ).setUp()

        logg.info(
            "Registrerte garbage collectors etter oppstart: ${
                ManagementFactory.getGarbageCollectorMXBeans().joinToString { it.name }
            }",
        )

        val snapshothenter =
            SpleisClientSnapshothenter(
                SpleisClient(
                    accessTokenGenerator = accessTokenGenerator,
                    configuration = configuration.spleisClientConfig,
                ),
            )

        val reservasjonshenter = (
            configuration.krrConfig.client?.let {
                KRRClientReservasjonshenter(
                    configuration = it,
                    accessTokenGenerator = accessTokenGenerator,
                )
            }
                ?: Reservasjonshenter { null }.also { logg.info("Bruker nulloperasjonsversjon av reservasjonshenter") }
        )

        ktorSetupCallback = {
            ApiModule(configuration.apiModuleConfiguration).setUpApi(
                daos = daos,
                tilgangsgrupper = configuration.tilgangsgrupper,
                meldingPubliserer = meldingPubliserer,
                gruppekontroll = MsGraphGruppekontroll(accessTokenGenerator),
                sessionFactory = sessionFactory,
                versjonAvKode = configuration.versjonAvKode,
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

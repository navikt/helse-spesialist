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
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.PersonhåndtererImpl
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.graphql.settOppGraphQLApi
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.db.FlywayMigrator
import no.nav.helse.spesialist.db.bootstrap.DBModule
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

fun main() {
    RapidApp.start(
        configuration = Configuration.fraEnv(System.getenv()),
        rapidsConnection =
            RapidApplication.create(
                env = System.getenv(),
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

        val tilgangskontrollørForReservasjon =
            TilgangskontrollørForReservasjon(MsGraphGruppekontroll(accessTokenGenerator), configuration.tilgangsgrupper)

        val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)

        val oppgaveService =
            OppgaveService(
                oppgaveDao = daos.oppgaveDao,
                reservasjonDao = daos.reservasjonDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = configuration.tilgangsgrupper,
                oppgaveRepository = daos.oppgaveRepository,
            )

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
                        oppgaveService = { oppgaveService },
                        godkjenningMediator = GodkjenningMediator(daos.opptegnelseDao),
                        subsumsjonsmelderProvider = { Subsumsjonsmelder(configuration.versjonAvKode, meldingPubliserer) },
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

        ktorSetupCallback = {
            val apiOppgaveService =
                ApiOppgaveService(
                    oppgaveDao = daos.oppgaveDao,
                    tilgangsgrupper = configuration.tilgangsgrupper,
                    oppgaveService = oppgaveService,
                )
            val stansAutomatiskBehandlinghåndterer =
                StansAutomatiskBehandlinghåndtererImpl(
                    daos.stansAutomatiskBehandlingDao,
                    daos.oppgaveDao,
                    daos.notatDao,
                    daos.dialogDao,
                )
            settOppGraphQLApi(
                daos = daos,
                sessionFactory = sessionFactory,
                saksbehandlerMediator =
                    SaksbehandlerMediator(
                        daos = daos,
                        versjonAvKode = configuration.versjonAvKode,
                        meldingPubliserer = meldingPubliserer,
                        oppgaveService = oppgaveService,
                        apiOppgaveService = apiOppgaveService,
                        tilgangsgrupper = configuration.tilgangsgrupper,
                        stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                        annulleringRepository = daos.annulleringRepository,
                        environmentToggles = configuration.environmentToggles,
                        featureToggles = featureToggles,
                        sessionFactory = sessionFactory,
                        tilgangskontroll = tilgangskontrollørForReservasjon,
                    ),
                apiOppgaveService = apiOppgaveService,
                godkjenninghåndterer =
                    GodkjenningService(
                        oppgaveDao = daos.oppgaveDao,
                        publiserer = meldingPubliserer,
                        oppgaveService = oppgaveService,
                        reservasjonDao = daos.reservasjonDao,
                        periodehistorikkDao = daos.periodehistorikkDao,
                        sessionFactory = sessionFactory,
                        featureToggles = featureToggles,
                    ),
                personhåndterer = PersonhåndtererImpl(publiserer = meldingPubliserer),
                dokumenthåndterer = DokumentMediator(daos.dokumentDao, meldingPubliserer),
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                behandlingstatistikk = BehandlingsstatistikkService(behandlingsstatistikkDao = daos.behandlingsstatistikkDao),
                snapshothenter =
                    SpleisClientSnapshothenter(
                        SpleisClient(
                            accessTokenGenerator = accessTokenGenerator,
                            configuration = configuration.spleisClientConfig,
                        ),
                    ),
                reservasjonshenter =
                    if (configuration.environmentToggles.brukDummyForKRR) {
                        logg.info("Bruker nulloperasjonsversjon av reservasjonshenter")
                        Reservasjonshenter { null }
                    } else {
                        KRRClientReservasjonshenter(
                            configuration = configuration.krrConfig,
                            accessTokenGenerator = accessTokenGenerator,
                        )
                    },
                tilgangsgrupper = configuration.tilgangsgrupper,
                meldingPubliserer = meldingPubliserer,
                featureToggles = featureToggles,
                azureConfig = configuration.azureConfig,
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

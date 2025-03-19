package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.MeldingPubliserer
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
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.graphql.settOppGraphQLApi
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.entraid.MsGraphGruppekontroll
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.db.bootstrap.DBModule
import org.slf4j.LoggerFactory
import java.lang.management.GarbageCollectorMXBean
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
        val accessTokenGenerator = EntraIDAccessTokenGenerator(configuration.accessTokenGeneratorConfig)

        val environmentToggles = configuration.environmentToggles
        val reservasjonshenter =
            if (environmentToggles.brukDummyForKRR) {
                logg.info("Bruker nulloperasjonsversjon av reservasjonshenter")
                Reservasjonshenter { null }
            } else {
                KRRClientReservasjonshenter(
                    configuration = configuration.krrConfig,
                    accessTokenGenerator = accessTokenGenerator,
                )
            }

        val gruppekontroll = MsGraphGruppekontroll(accessTokenGenerator)
        val snapshothenter =
            SpleisClientSnapshothenter(
                SpleisClient(
                    accessTokenGenerator = accessTokenGenerator,
                    configuration = configuration.spleisClientConfig,
                ),
            )
        val azureConfig = configuration.azureConfig
        val tilgangsgrupper = configuration.tilgangsgrupper
        val versjonAvKode = configuration.versjonAvKode
        val featureToggles = UnleashFeatureToggles(configuration = configuration.unleashFeatureToggles)
        val dbModuleConfiguration = configuration.dbConfig
        val stikkprøver = configuration.stikkprøver

        val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

        val dbModule = DBModule(dbModuleConfiguration)
        val daos = dbModule.daos
        val sessionFactory = dbModule.sessionFactory

        val meldingPubliserer: MeldingPubliserer
        val meldingMediator: MeldingMediator
        val personhåndterer: Personhåndterer
        val saksbehandlerMediator: SaksbehandlerMediator
        val oppgaveService: OppgaveService
        val apiOppgaveService: ApiOppgaveService
        val dokumentMediator: DokumentMediator
        val subsumsjonsmelder: Subsumsjonsmelder

        val behandlingsstatistikkService =
            BehandlingsstatistikkService(behandlingsstatistikkDao = daos.behandlingsstatistikkDao)
        val godkjenningMediator = GodkjenningMediator(daos.opptegnelseDao)
        val stansAutomatiskBehandlinghåndterer =
            StansAutomatiskBehandlinghåndtererImpl(
                daos.stansAutomatiskBehandlingDao,
                daos.oppgaveDao,
                daos.notatDao,
                daos.dialogDao,
            )

        val godkjenningService: GodkjenningService

        rapidsConnection.register(
            object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    dbModule.flywayMigrator.migrate()
                }
            },
        )
        meldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)
        subsumsjonsmelder = Subsumsjonsmelder(versjonAvKode, meldingPubliserer)
        oppgaveService =
            OppgaveService(
                oppgaveDao = daos.oppgaveDao,
                reservasjonDao = daos.reservasjonDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
                oppgaveRepository = daos.oppgaveRepository,
            )
        val kommandofabrikk =
            Kommandofabrikk(
                oppgaveService = { oppgaveService },
                godkjenningMediator = godkjenningMediator,
                subsumsjonsmelderProvider = { subsumsjonsmelder },
                stikkprøver = stikkprøver,
                featureToggles = featureToggles,
            )
        apiOppgaveService =
            ApiOppgaveService(
                oppgaveDao = daos.oppgaveDao,
                tilgangsgrupper = tilgangsgrupper,
                oppgaveService = oppgaveService,
            )
        meldingMediator =
            MeldingMediator(
                sessionFactory = sessionFactory,
                personDao = daos.personDao,
                commandContextDao = daos.commandContextDao,
                meldingDao = daos.meldingDao,
                meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
                kommandofabrikk = kommandofabrikk,
                dokumentDao = daos.dokumentDao,
                varselRepository =
                    VarselRepository(
                        varselDao = daos.varselDao,
                        definisjonDao = daos.definisjonDao,
                    ),
                poisonPillDao = daos.poisonPillDao,
                environmentToggles = environmentToggles,
            )
        personhåndterer = PersonhåndtererImpl(publiserer = meldingPubliserer)
        RiverSetup(rapidsConnection, meldingMediator, daos.meldingDuplikatkontrollDao).setUp()
        saksbehandlerMediator =
            SaksbehandlerMediator(
                daos = daos,
                versjonAvKode = versjonAvKode,
                meldingPubliserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                apiOppgaveService = apiOppgaveService,
                tilgangsgrupper = tilgangsgrupper,
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                annulleringRepository = daos.annulleringRepository,
                environmentToggles = environmentToggles,
                featureToggles = featureToggles,
                sessionFactory = sessionFactory,
                tilgangskontroll = tilgangskontrollørForReservasjon,
            )
        dokumentMediator = DokumentMediator(daos.dokumentDao, meldingPubliserer)
        godkjenningService =
            GodkjenningService(
                oppgaveDao = daos.oppgaveDao,
                publiserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                reservasjonDao = daos.reservasjonDao,
                periodehistorikkDao = daos.periodehistorikkDao,
                sessionFactory = sessionFactory,
                featureToggles = featureToggles,
            )

        val beans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
        logg.info("Registrerte garbage collectors etter oppstart: ${beans.joinToString { it.name }}")

        ktorSetupCallback = {
            settOppGraphQLApi(
                daos = daos,
                sessionFactory = sessionFactory,
                saksbehandlerMediator = saksbehandlerMediator,
                apiOppgaveService = apiOppgaveService,
                godkjenninghåndterer = godkjenningService,
                personhåndterer = personhåndterer,
                dokumenthåndterer = dokumentMediator,
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                behandlingstatistikk = behandlingsstatistikkService,
                snapshothenter = snapshothenter,
                reservasjonshenter = reservasjonshenter,
                tilgangsgrupper = tilgangsgrupper,
                meldingPubliserer = meldingPubliserer,
                featureToggles = featureToggles,
                azureConfig = azureConfig,
            )
        }

        rapidsConnection.start()
    }
}

private val logg = LoggerFactory.getLogger("SpesialistApp")

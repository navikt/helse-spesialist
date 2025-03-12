package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.FeatureToggles
import no.nav.helse.Gruppekontroll
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.Environment
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
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.debugMinneApi
import no.nav.helse.spesialist.api.bootstrap.installPlugins
import no.nav.helse.spesialist.api.graphql.settOppGraphQLApi
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.db.bootstrap.DBModule
import org.slf4j.LoggerFactory
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import kotlin.random.Random

class SpesialistApp(
    private val env: Environment,
    gruppekontroll: Gruppekontroll,
    private val snapshothenter: Snapshothenter,
    private val azureConfig: AzureConfig,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val reservasjonshenter: Reservasjonshenter,
    private val versjonAvKode: String,
    private val featureToggles: FeatureToggles,
    dbModuleConfiguration: DBModule.Configuration,
) : RapidsConnection.StatusListener {
    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

    private val dbModule = DBModule(dbModuleConfiguration)
    private val daos = dbModule.daos
    private val sessionFactory = dbModule.sessionFactory

    private lateinit var meldingPubliserer: MeldingPubliserer
    private lateinit var meldingMediator: MeldingMediator
    private lateinit var personhåndterer: Personhåndterer
    private lateinit var saksbehandlerMediator: SaksbehandlerMediator
    private lateinit var oppgaveService: OppgaveService
    private lateinit var apiOppgaveService: ApiOppgaveService
    private lateinit var dokumentMediator: DokumentMediator
    private lateinit var subsumsjonsmelder: Subsumsjonsmelder

    private val behandlingsstatistikkService =
        BehandlingsstatistikkService(behandlingsstatistikkDao = daos.behandlingsstatistikkDao)
    private val godkjenningMediator = GodkjenningMediator(daos.opptegnelseDao)
    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            daos.stansAutomatiskBehandlingDao,
            daos.oppgaveDao,
            daos.notatDao,
            daos.dialogDao,
        )

    private lateinit var godkjenningService: GodkjenningService

    private val plukkTilManuell: PlukkTilManuell<String> = (
        {
            it?.let {
                val divisor = it.toInt()
                require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
                Random.nextInt(divisor) == 0
            } ?: false
        }
    )

    private val stikkprøver =
        object : Stikkprøver {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FGB_DIVISOR"])

            override fun utsFlereArbeidsgivereForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FORLENGELSE_DIVISOR"])

            override fun utsEnArbeidsgiverFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FGB_DIVISOR"])

            override fun utsEnArbeidsgiverForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FORLENGELSE_DIVISOR"])

            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() =
                plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FGB_DIVISOR"])

            override fun fullRefusjonFlereArbeidsgivereForlengelse() =
                plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FORLENGELSE_DIVISOR"])

            override fun fullRefusjonEnArbeidsgiver() = plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_EN_AG_DIVISOR"])
        }

    private val kommandofabrikk =
        Kommandofabrikk(
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            subsumsjonsmelderProvider = { subsumsjonsmelder },
            stikkprøver = stikkprøver,
            featureToggles = featureToggles,
        )

    fun start(rapidsConnection: RapidsConnection) {
        rapidsConnection.register(this)
        meldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)
        oppgaveService =
            OppgaveService(
                oppgaveDao = daos.oppgaveDao,
                reservasjonDao = daos.reservasjonDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
                oppgaveRepository = daos.oppgaveRepository,
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
                env = env,
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
                env = env,
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
        subsumsjonsmelder = Subsumsjonsmelder(versjonAvKode, meldingPubliserer)

        loggOppstartsmelding()

        rapidsConnection.start()
    }

    private fun loggOppstartsmelding() {
        val beans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
        logg.info("Registrerte garbage collectors etter oppstart: ${beans.joinToString { it.name }}")
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dbModule.flywayMigrator.migrate()
    }

    fun konfigurerKtorApp(application: Application) {
        application.apply {
            installPlugins()
            azureAdAppAuthentication(azureConfig)
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
            )

            routing {
                webSocketsApi()
                debugMinneApi()
            }
        }
    }
}

private val logg = LoggerFactory.getLogger("SpesialistApp")

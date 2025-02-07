package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.DBRepositories
import no.nav.helse.db.DataSourceBuilder
import no.nav.helse.db.TransactionalSessionFactory
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
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.bootstrap.ApiAvhengigheter
import no.nav.helse.spesialist.api.bootstrap.Bootstrap
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import org.slf4j.LoggerFactory
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import kotlin.random.Random

private val logg = LoggerFactory.getLogger("SpesialistApp")

class SpesialistApp(
    private val env: Environment,
    gruppekontroll: Gruppekontroll,
    snapshotClient: ISnapshotClient,
    private val azureConfig: AzureConfig,
    private val tilgangsgrupper: Tilgangsgrupper,
    reservasjonClient: ReservasjonClient,
    private val versjonAvKode: String,
    private val featureToggles: FeatureToggles,
) : RapidsConnection.StatusListener {
    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()
    private val repositories = DBRepositories(dataSource, tilgangskontrollørForReservasjon)
    private val sessionFactory = TransactionalSessionFactory(dataSource, tilgangskontrollørForReservasjon)

    private val oppgaveDao = repositories.oppgaveDao
    private val periodehistorikkDao = repositories.periodehistorikkDao
    private val saksbehandlerDao = repositories.saksbehandlerDao
    private val tildelingDao = repositories.tildelingDao
    private val reservasjonDao = repositories.reservasjonDao
    private val opptegnelseDao = repositories.opptegnelseDao
    private val behandlingsstatistikkDao = repositories.behandlingsstatistikkDao
    private val notatDao = repositories.notatDao
    private val dialogDao = repositories.dialogDao
    private val totrinnsvurderingDao = repositories.totrinnsvurderingDao
    private val dokumentDao = repositories.dokumentDao
    private val stansAutomatiskBehandlingDao = repositories.stansAutomatiskBehandlingDao

    private lateinit var meldingMediator: MeldingMediator
    private lateinit var personhåndterer: Personhåndterer
    private lateinit var saksbehandlerMediator: SaksbehandlerMediator
    private lateinit var oppgaveService: OppgaveService
    private lateinit var apiOppgaveService: ApiOppgaveService
    private lateinit var dokumentMediator: DokumentMediator
    private lateinit var subsumsjonsmelder: Subsumsjonsmelder

    private val behandlingsstatistikkService =
        BehandlingsstatistikkService(behandlingsstatistikkDao = behandlingsstatistikkDao)
    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            stansAutomatiskBehandlingDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        )
    private val totrinnsvurderingService =
        TotrinnsvurderingService(
            totrinnsvurderingDao = totrinnsvurderingDao,
            periodehistorikkDao = periodehistorikkDao,
            dialogDao = dialogDao,
        )

    private lateinit var godkjenningService: GodkjenningService

    private val apiAvhengigheter =
        ApiAvhengigheter(
            saksbehandlerhåndtererProvider = { saksbehandlerMediator },
            apiOppgaveServiceProvider = { apiOppgaveService },
            totrinnsvurderinghåndterer = { totrinnsvurderingService },
            godkjenninghåndtererProvider = { godkjenningService },
            personhåndtererProvider = { personhåndterer },
            dokumenthåndtererProvider = { dokumentMediator },
            stansAutomatiskBehandlinghåndterer = { stansAutomatiskBehandlinghåndterer },
            behandlingstatistikk = behandlingsstatistikkService,
            snapshotClient = snapshotClient,
            avviksvurderinghenter = repositories.avviksvurderingDao,
        )

    private val bootstrap =
        Bootstrap(
            repositories = repositories,
            sessionFactory = sessionFactory,
            avhengigheter = apiAvhengigheter,
            reservasjonClient = reservasjonClient,
            tilgangsgrupper = tilgangsgrupper,
        )

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
        val meldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)
        oppgaveService =
            OppgaveService(
                oppgaveDao = oppgaveDao,
                tildelingDao = tildelingDao,
                reservasjonDao = reservasjonDao,
                opptegnelseDao = opptegnelseDao,
                totrinnsvurderingDao = totrinnsvurderingDao,
                saksbehandlerDao = saksbehandlerDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
                repositories = repositories,
            )
        apiOppgaveService =
            ApiOppgaveService(
                oppgaveDao = oppgaveDao,
                tilgangsgrupper = tilgangsgrupper,
                oppgaveService = oppgaveService,
            )
        meldingMediator =
            MeldingMediator(
                sessionFactory = sessionFactory,
                personDao = repositories.personDao,
                commandContextDao = repositories.commandContextDao,
                meldingDao = repositories.meldingDao,
                meldingDuplikatkontrollDao = repositories.meldingDuplikatkontrollDao,
                kommandofabrikk = kommandofabrikk,
                dokumentDao = dokumentDao,
                varselRepository =
                    VarselRepository(
                        varselDao = repositories.varselDao,
                        definisjonDao = repositories.definisjonDao,
                    ),
                poisonPills = repositories.poisonPillDao.poisonPills(),
                env = env,
            )
        personhåndterer = PersonhåndtererImpl(publiserer = meldingPubliserer)
        RiverSetup(rapidsConnection, meldingMediator, repositories.meldingDuplikatkontrollDao).setUp()
        saksbehandlerMediator =
            SaksbehandlerMediator(
                repositories = repositories,
                versjonAvKode = versjonAvKode,
                meldingPubliserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                apiOppgaveService = apiOppgaveService,
                tilgangsgrupper = tilgangsgrupper,
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                annulleringRepository = repositories.annulleringRepository,
                env = env,
                featureToggles = featureToggles,
            )
        dokumentMediator = DokumentMediator(dokumentDao, meldingPubliserer)
        godkjenningService =
            GodkjenningService(
                oppgaveDao = oppgaveDao,
                overstyringDao = repositories.overstyringDao,
                publiserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                reservasjonDao = reservasjonDao,
                periodehistorikkDao = periodehistorikkDao,
                saksbehandlerDao = saksbehandlerDao,
                totrinnsvurderingService =
                    TotrinnsvurderingService(
                        totrinnsvurderingDao = totrinnsvurderingDao,
                        periodehistorikkDao = periodehistorikkDao,
                        dialogDao = dialogDao,
                    ),
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
        dataSourceBuilder.migrate()
    }

    fun ktorApp(application: Application) = bootstrap.ktorApp(application, azureConfig, env)
}

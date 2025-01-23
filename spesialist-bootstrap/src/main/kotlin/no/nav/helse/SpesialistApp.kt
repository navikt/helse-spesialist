package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PgAnnulleringRepository
import no.nav.helse.db.PgAvviksvurderingDao
import no.nav.helse.db.PgDialogDao
import no.nav.helse.db.PgNotatDao
import no.nav.helse.db.PgOppgaveDao
import no.nav.helse.db.PgPeriodehistorikkDao
import no.nav.helse.db.PgTotrinnsvurderingDao
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.db.RepositoryFactoryImpl
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.dokument.PgDokumentDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.spesialist.api.AzureConfig
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
) : RapidsConnection.StatusListener {
    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()
    private val repositoryFactory = RepositoryFactoryImpl(dataSource)

    private val oppgaveDao = PgOppgaveDao(dataSource)
    private val historikkinnslagRepository = PgPeriodehistorikkDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    private val notatDao = PgNotatDao(dataSource)
    private val dialogDao = PgDialogDao(dataSource)
    private val totrinnsvurderingDao = PgTotrinnsvurderingDao(dataSource)
    private val dokumentDao = PgDokumentDao(dataSource)
    private val stansAutomatiskBehandlingDao = StansAutomatiskBehandlingDao(dataSource)

    private lateinit var meldingMediator: MeldingMediator
    private lateinit var saksbehandlerMediator: SaksbehandlerMediator
    private lateinit var oppgaveService: OppgaveService
    private lateinit var dokumentMediator: DokumentMediator
    private lateinit var subsumsjonsmelder: Subsumsjonsmelder

    private val behandlingsstatistikkService =
        BehandlingsstatistikkService(behandlingsstatistikkDao = behandlingsstatistikkDao)
    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            historikkinnslagRepository,
            oppgaveDao,
            notatDao,
            dialogDao,
        ) { subsumsjonsmelder }
    private val totrinnsvurderingService =
        TotrinnsvurderingService(
            totrinnsvurderingDao = totrinnsvurderingDao,
            oppgaveDao = oppgaveDao,
            periodehistorikkDao = historikkinnslagRepository,
            dialogDao = dialogDao,
        )

    private lateinit var godkjenningService: GodkjenningService

    private val apiAvhengigheter =
        ApiAvhengigheter(
            saksbehandlerhåndtererProvider = { saksbehandlerMediator },
            oppgavehåndtererProvider = { oppgaveService },
            totrinnsvurderinghåndterer = { totrinnsvurderingService },
            godkjenninghåndtererProvider = { godkjenningService },
            personhåndtererProvider = { meldingMediator },
            dokumenthåndtererProvider = { dokumentMediator },
            stansAutomatiskBehandlinghåndterer = { stansAutomatiskBehandlingMediator },
            behandlingstatistikk = behandlingsstatistikkService,
            snapshotClient = snapshotClient,
            avviksvurderinghenter = PgAvviksvurderingDao(dataSource),
        )

    private val bootstrap = Bootstrap(dataSource, apiAvhengigheter, reservasjonClient, tilgangsgrupper)

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
            dataSource = dataSource,
            repositoryFactory = repositoryFactory,
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            subsumsjonsmelderProvider = { subsumsjonsmelder },
            stikkprøver = stikkprøver,
        )

    fun start(rapidsConnection: RapidsConnection) {
        rapidsConnection.register(this)
        val meldingPubliserer = MessageContextMeldingPubliserer(rapidsConnection)
        oppgaveService =
            OppgaveService(
                oppgaveDao = oppgaveDao,
                tildelingRepository = tildelingDao,
                reservasjonRepository = reservasjonDao,
                opptegnelseRepository = opptegnelseDao,
                totrinnsvurderingDao = totrinnsvurderingDao,
                saksbehandlerRepository = saksbehandlerDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
            )
        meldingMediator =
            MeldingMediator(
                dataSource = dataSource,
                repositoryFactory = repositoryFactory,
                publiserer = meldingPubliserer,
                kommandofabrikk = kommandofabrikk,
                poisonPills = PoisonPillDao(dataSource).poisonPills(),
            )
        RiverSetup(dataSource, rapidsConnection, meldingMediator).setUp()
        saksbehandlerMediator =
            SaksbehandlerMediator(
                dataSource = dataSource,
                repositoryFactory = repositoryFactory,
                versjonAvKode = versjonAvKode,
                meldingPubliserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                tilgangsgrupper = tilgangsgrupper,
                stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
                totrinnsvurderingService = totrinnsvurderingService,
                annulleringRepository = PgAnnulleringRepository(dataSource),
            )
        dokumentMediator = DokumentMediator(dokumentDao, meldingPubliserer)
        godkjenningService =
            GodkjenningService(
                dataSource = dataSource,
                publiserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                saksbehandlerRepository = saksbehandlerDao,
                tilgangskontroll = tilgangskontrollørForReservasjon,
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

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataSource.close()
    }

    fun ktorApp(application: Application) = bootstrap.ktorApp(application, azureConfig, env)
}

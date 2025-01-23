package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.DBRepositories
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
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.modell.varsel.VarselRepository
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
    private val repositories = DBRepositories(dataSource)

    private val oppgaveDao = repositories.oppgaveDao
    private val periodehistorikkDao = repositories.periodehistorikkDao
    private val saksbehandlerDao = repositories.saksbehandlerDao
    private val tildelingDao = repositories.tildelingDao
    private val reservasjonDao = repositories.reservasjonDao
    private val opptegnelseRepository = repositories.opptegnelseRepository
    private val behandlingsstatistikkDao = repositories.behandlingsstatistikkDao
    private val notatDao = repositories.notatDao
    private val dialogDao = repositories.dialogDao
    private val totrinnsvurderingDao = repositories.totrinnsvurderingDao
    private val dokumentDao = repositories.dokumentDao
    private val stansAutomatiskBehandlingDao = repositories.stansAutomatiskBehandlingDao

    private lateinit var meldingMediator: MeldingMediator
    private lateinit var saksbehandlerMediator: SaksbehandlerMediator
    private lateinit var oppgaveService: OppgaveService
    private lateinit var dokumentMediator: DokumentMediator
    private lateinit var subsumsjonsmelder: Subsumsjonsmelder

    private val behandlingsstatistikkService =
        BehandlingsstatistikkService(behandlingsstatistikkDao = behandlingsstatistikkDao)
    private val godkjenningMediator = GodkjenningMediator(opptegnelseRepository)
    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            periodehistorikkDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        ) { subsumsjonsmelder }
    private val totrinnsvurderingService =
        TotrinnsvurderingService(
            totrinnsvurderingDao = totrinnsvurderingDao,
            oppgaveDao = oppgaveDao,
            periodehistorikkDao = periodehistorikkDao,
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
            avviksvurderinghenter = repositories.avviksvurderingDao,
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
            repositories = repositories,
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
                tildelingDao = tildelingDao,
                reservasjonDao = reservasjonDao,
                opptegnelseRepository = opptegnelseRepository,
                totrinnsvurderingDao = totrinnsvurderingDao,
                saksbehandlerDao = saksbehandlerDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
                repositories = repositories,
            )
        meldingMediator =
            MeldingMediator(
                dataSource = dataSource,
                repositories = repositories,
                publiserer = meldingPubliserer,
                kommandofabrikk = kommandofabrikk,
                poisonPills = repositories.poisonPillDao.poisonPills(),
                commandContextDao = repositories.commandContextDao,
                dokumentDao = dokumentDao,
                personDao = repositories.personDao,
                varselRepository =
                    VarselRepository(
                        varselDao = repositories.varselDao,
                        definisjonDao = repositories.definisjonDao,
                    ),
                meldingDao = repositories.meldingDao,
                meldingDuplikatkontrollDao = repositories.meldingDuplikatkontrollDao,
            )
        RiverSetup(rapidsConnection, meldingMediator, repositories.meldingDuplikatkontrollDao).setUp()
        saksbehandlerMediator =
            SaksbehandlerMediator(
                dataSource = dataSource,
                repositories = repositories,
                versjonAvKode = versjonAvKode,
                meldingPubliserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                tilgangsgrupper = tilgangsgrupper,
                stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
                totrinnsvurderingService = totrinnsvurderingService,
                annulleringRepository = repositories.annulleringRepository,
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
                        oppgaveDao = oppgaveDao,
                        totrinnsvurderingDao = totrinnsvurderingDao,
                        periodehistorikkDao = periodehistorikkDao,
                        dialogDao = dialogDao,
                    ),
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

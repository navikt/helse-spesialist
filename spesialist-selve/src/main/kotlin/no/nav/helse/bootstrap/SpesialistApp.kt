package no.nav.helse.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.DataSourceBuilder
import no.nav.helse.Gruppekontroll
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.BehandlingsstatistikkService
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatRepository
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vergemål.VergemålApiDao
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import org.slf4j.LoggerFactory
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random

private val logg = LoggerFactory.getLogger("SpesialistApp")

internal class SpesialistApp(
    private val env: Environment,
    gruppekontroll: Gruppekontroll,
    snapshotClient: ISnapshotClient,
    private val azureConfig: AzureConfig,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val reservasjonClient: ReservasjonClient,
    private val versjonAvKode: String,
    private val rapidsConnectionProvider: () -> RapidsConnection,
) : RapidsConnection.StatusListener {
    private val rapidsConnection: RapidsConnection by lazy { rapidsConnectionProvider() }

    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()

    private val personDao = PersonDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val utbetalingDao = UtbetalingDao(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingApiDao = TildelingDao(dataSource)
    private val tildelingDao =
        no.nav.helse.db
            .TildelingDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val overstyringApiDao = OverstyringApiDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    private val egenAnsattApiDao = EgenAnsattApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    private val notatDao = NotatDao(dataSource)
    private val totrinnsvurderingApiDao = TotrinnsvurderingApiDao(dataSource)
    private val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val apiVarselRepository = ApiVarselRepository(dataSource)
    private val meldingDao = MeldingDao(dataSource)
    private val dokumentDao = DokumentDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)
    private val påVentApiDao = PåVentApiDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
    private val automatiseringDao = AutomatiseringDao(dataSource)
    private val stansAutomatiskBehandlingDao = StansAutomatiskBehandlingDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val vergemålApiDao = VergemålApiDao(dataSource)

    private lateinit var meldingMediator: MeldingMediator
    private lateinit var saksbehandlerMediator: SaksbehandlerMediator
    private lateinit var oppgaveService: OppgaveService
    private lateinit var dokumentMediator: DokumentMediator
    private lateinit var subsumsjonsmelder: Subsumsjonsmelder

    private val behandlingsstatistikkService = BehandlingsstatistikkService(behandlingsstatistikkDao = behandlingsstatistikkDao)
    private val godkjenningMediator =
        GodkjenningMediator(
            vedtakDao = vedtakDao,
            opptegnelseDao = opptegnelseDao,
            oppgaveDao = oppgaveDao,
            utbetalingDao = utbetalingDao,
            meldingDao = meldingDao,
            generasjonDao = generasjonDao,
        )
    private val notatRepository = NotatRepository(notatDao = notatDao)
    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            periodehistorikkDao,
            oppgaveDao,
            utbetalingDao,
            notatRepository,
        ) { subsumsjonsmelder }
    private val totrinnsvurderingMediator =
        TotrinnsvurderingMediator(
            dao = totrinnsvurderingDao,
            oppgaveDao = oppgaveDao,
            periodehistorikkDao = periodehistorikkDao,
            notatRepository = notatRepository,
        )

    private val snapshotService = SnapshotService(snapshotDao = snapshotApiDao, snapshotClient = snapshotClient)
    private lateinit var godkjenningService: GodkjenningService

    private val avviksvurderinghenter =
        object : Avviksvurderinghenter {
            override fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering? =
                avviksvurderingDao.finnAvviksvurdering(vilkårsgrunnlagId)
        }

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

    private val automatisering =
        Automatisering(
            risikovurderingDao = risikovurderingDao,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlingMediator,
            automatiseringDao = automatiseringDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            vergemålDao = vergemålDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            overstyringDao = overstyringDao,
            stikkprøver = stikkprøver,
            meldingDao = meldingDao,
            generasjonDao = generasjonDao,
            egenAnsattDao = egenAnsattDao,
        )

    private val kommandofabrikk =
        Kommandofabrikk(
            dataSource = dataSource,
            snapshotClient = snapshotClient,
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            automatisering = automatisering,
        )

    internal fun ktorApp(application: Application) {
        application.apply {
            installPlugins()
            azureAdAppAuthentication(azureConfig, env)
            graphQLApi(
                personApiDao = personApiDao,
                egenAnsattApiDao = egenAnsattApiDao,
                tildelingDao = tildelingApiDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselRepository = apiVarselRepository,
                oppgaveApiDao = oppgaveApiDao,
                periodehistorikkDao = periodehistorikkDao,
                notatDao = notatDao,
                totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                påVentApiDao = påVentApiDao,
                vergemålApiDao = vergemålApiDao,
                reservasjonClient = reservasjonClient,
                avviksvurderinghenter = avviksvurderinghenter,
                skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
                kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
                beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
                snapshotService = snapshotService,
                behandlingsstatistikkMediator = behandlingsstatistikkService,
                saksbehandlerhåndtererProvider = { saksbehandlerMediator },
                oppgavehåndtererProvider = { oppgaveService },
                totrinnsvurderinghåndterer = totrinnsvurderingMediator,
                godkjenninghåndtererProvider = { godkjenningService },
                personhåndtererProvider = { meldingMediator },
                dokumenthåndtererProvider = { dokumentMediator },
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlingMediator,
            )

            routing {
                webSocketsApi()
            }
        }
    }

    fun start() {
        rapidsConnection.register(this)
        oppgaveService =
            OppgaveService(
                meldingDao = meldingDao,
                oppgaveDao = oppgaveDao,
                tildelingDao = tildelingDao,
                reservasjonDao = reservasjonDao,
                opptegnelseDao = opptegnelseDao,
                totrinnsvurderingRepository = totrinnsvurderingDao,
                saksbehandlerRepository = saksbehandlerDao,
                rapidsConnection = rapidsConnection,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
            )
        meldingMediator =
            MeldingMediator(
                dataSource = dataSource,
                rapidsConnection = rapidsConnection,
                kommandofabrikk = kommandofabrikk,
                avviksvurderingDao = avviksvurderingDao,
                stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
                poisonPills = emptyMap(),
            )
        saksbehandlerMediator =
            SaksbehandlerMediator(
                dataSource = dataSource,
                versjonAvKode = versjonAvKode,
                rapidsConnection = rapidsConnection,
                oppgaveService = oppgaveService,
                tilgangsgrupper = tilgangsgrupper,
                stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
            )
        dokumentMediator = DokumentMediator(dokumentDao, rapidsConnection)
        godkjenningService =
            GodkjenningService(
                dataSource = dataSource,
                rapidsConnection = rapidsConnection,
                oppgaveService = oppgaveService,
                saksbehandlerRepository = saksbehandlerDao,
            )
        subsumsjonsmelder = Subsumsjonsmelder(versjonAvKode, rapidsConnection)

        rapidsConnection.start().also {
            val beans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
            logg.info("Registrerte garbage collectors etter oppstart: ${beans.joinToString { it.name }}")
        }
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataSource.close()
    }
}

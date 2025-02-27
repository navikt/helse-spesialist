package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
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
import no.nav.helse.spesialist.api.bootstrap.ApiAvhengigheter
import no.nav.helse.spesialist.api.bootstrap.Bootstrap
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.DataSourceBuilder
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import org.slf4j.LoggerFactory
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import kotlin.random.Random

private val logg = LoggerFactory.getLogger("SpesialistApp")

class SpesialistApp(
    private val env: Environment,
    gruppekontroll: Gruppekontroll,
    snapshothenter: Snapshothenter,
    private val azureConfig: AzureConfig,
    private val tilgangsgrupper: Tilgangsgrupper,
    reservasjonshenter: Reservasjonshenter,
    private val versjonAvKode: String,
    private val featureToggles: FeatureToggles,
) : RapidsConnection.StatusListener {
    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()
    private val daos = DBDaos(dataSource)
    private val sessionFactory = TransactionalSessionFactory(dataSource)

    private val oppgaveDao = daos.oppgaveDao
    private val periodehistorikkDao = daos.periodehistorikkDao
    private val tildelingDao = daos.tildelingDao
    private val reservasjonDao = daos.reservasjonDao
    private val opptegnelseDao = daos.opptegnelseDao
    private val behandlingsstatistikkDao = daos.behandlingsstatistikkDao
    private val notatDao = daos.notatDao
    private val dialogDao = daos.dialogDao
    private val dokumentDao = daos.dokumentDao
    private val stansAutomatiskBehandlingDao = daos.stansAutomatiskBehandlingDao

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

    private lateinit var godkjenningService: GodkjenningService

    private val apiAvhengigheter =
        ApiAvhengigheter(
            saksbehandlerMediatorProvider = { saksbehandlerMediator },
            apiOppgaveServiceProvider = { apiOppgaveService },
            godkjenninghåndtererProvider = { godkjenningService },
            personhåndtererProvider = { personhåndterer },
            dokumenthåndtererProvider = { dokumentMediator },
            stansAutomatiskBehandlinghåndterer = { stansAutomatiskBehandlinghåndterer },
            behandlingstatistikk = behandlingsstatistikkService,
            snapshothenter = snapshothenter,
        )

    private val bootstrap =
        Bootstrap(
            daos = daos,
            sessionFactory = sessionFactory,
            avhengigheter = apiAvhengigheter,
            reservasjonshenter = reservasjonshenter,
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
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = tilgangskontrollørForReservasjon,
                tilgangsgrupper = tilgangsgrupper,
                daos = daos,
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
                personDao = daos.personDao,
                commandContextDao = daos.commandContextDao,
                meldingDao = daos.meldingDao,
                meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
                kommandofabrikk = kommandofabrikk,
                dokumentDao = dokumentDao,
                varselRepository =
                    VarselRepository(
                        varselDao = daos.varselDao,
                        definisjonDao = daos.definisjonDao,
                    ),
                poisonPills = daos.poisonPillDao.poisonPills(),
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
        dokumentMediator = DokumentMediator(dokumentDao, meldingPubliserer)
        godkjenningService =
            GodkjenningService(
                oppgaveDao = oppgaveDao,
                overstyringDao = daos.overstyringDao,
                publiserer = meldingPubliserer,
                oppgaveService = oppgaveService,
                reservasjonDao = reservasjonDao,
                periodehistorikkDao = periodehistorikkDao,
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
        dataSourceBuilder.migrate()
    }

    fun ktorApp(application: Application) = bootstrap.ktorApp(application, azureConfig, env)
}

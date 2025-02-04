package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.DBRepositories
import no.nav.helse.db.TransactionalSessionFactory
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.util.testEnv
import java.util.UUID
import javax.sql.DataSource

internal class TestMediator(
    testRapid: TestRapid,
    dataSource: DataSource,
) {
    private val environment = object : Environment, Map<String, String> by emptyMap() {
        override val erLokal = false
        override val erDev = false
        override val erProd = false
    }
    private val repositories = DBRepositories(dataSource)
    private val opptegnelseDao = repositories.opptegnelseDao
    private val oppgaveDao = repositories.oppgaveDao
    private val periodehistorikkDao = repositories.periodehistorikkDao
    private val overstyringDao = repositories.overstyringDao
    private val totrinnsvurderingDao = repositories.totrinnsvurderingDao
    private val saksbehandlerDao = repositories.saksbehandlerDao
    private val tildelingDao = repositories.tildelingDao
    private val notatDao = repositories.notatDao
    private val dialogDao = repositories.dialogDao
    private val annulleringRepository = repositories.annulleringRepository
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            repositories.stansAutomatiskBehandlingDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        )

    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = repositories.oppgaveDao,
            tildelingDao = tildelingDao,
            reservasjonDao = repositories.reservasjonDao,
            opptegnelseDao = opptegnelseDao,
            totrinnsvurderingDao = totrinnsvurderingDao,
            saksbehandlerDao = saksbehandlerDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = tilgangsgrupper,
            repositories = repositories,
        )
    private val apiOppgaveService = ApiOppgaveService(
        oppgaveDao = repositories.oppgaveDao,
        tilgangsgrupper = tilgangsgrupper,
        oppgaveService = oppgaveService
    )

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            repositories = repositories,
            versjonAvKode = "versjonAvKode",
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            tilgangsgrupper = tilgangsgrupper,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            totrinnsvurderingService =
                TotrinnsvurderingService(
                    totrinnsvurderingDao = totrinnsvurderingDao,
                    oppgaveDao = oppgaveDao,
                    periodehistorikkDao = periodehistorikkDao,
                    dialogDao = dialogDao,
                ),
            annulleringRepository = annulleringRepository,
            env = environment,
            featureToggles = object : FeatureToggles{}
        )

    private val stikkprøver =
        object : Stikkprøver {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = false

            override fun utsFlereArbeidsgivereForlengelse() = false

            override fun utsEnArbeidsgiverFørstegangsbehandling() = false

            override fun utsEnArbeidsgiverForlengelse() = false

            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false

            override fun fullRefusjonFlereArbeidsgivereForlengelse() = false

            override fun fullRefusjonEnArbeidsgiver() = false
        }

    private val kommandofabrikk =
        Kommandofabrikk(
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            subsumsjonsmelderProvider = { Subsumsjonsmelder("versjonAvKode", meldingPubliserer) },
            stikkprøver = stikkprøver,
            featureToggles = object : FeatureToggles{}
        )


    init {
        val meldingMediator = MeldingMediator(
            sessionFactory = TransactionalSessionFactory(dataSource),
            personDao = repositories.personDao,
            commandContextDao = repositories.commandContextDao,
            meldingDao = repositories.meldingDao,
            meldingDuplikatkontrollDao = repositories.meldingDuplikatkontrollDao,
            kommandofabrikk = kommandofabrikk,
            dokumentDao = repositories.dokumentDao,
            varselRepository = VarselRepository(
                varselDao = repositories.varselDao,
                definisjonDao = repositories.definisjonDao
            ),
            poisonPills = PoisonPills(emptyMap()),
            env = environment,
        )
        RiverSetup(testRapid, meldingMediator, repositories.meldingDuplikatkontrollDao).setUp()
    }

    internal fun overstyringstyperForVedtaksperiode(vedtaksperiodeId: UUID) =
        overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)

    internal fun håndter(
        handling: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        saksbehandlerMediator.håndter(handling, saksbehandlerFraApi)
    }
}

package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.bootstrap.Environment
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
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.TransactionalSessionFactory
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
    private val daos = DBDaos(dataSource)
    private val opptegnelseDao = daos.opptegnelseDao
    private val oppgaveDao = daos.oppgaveDao
    private val overstyringDao = daos.overstyringDao
    private val tildelingDao = daos.tildelingDao
    private val notatDao = daos.notatDao
    private val dialogDao = daos.dialogDao
    private val annulleringRepository = daos.annulleringRepository
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            daos.stansAutomatiskBehandlingDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        )

    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = daos.oppgaveDao,
            tildelingDao = tildelingDao,
            reservasjonDao = daos.reservasjonDao,
            opptegnelseDao = opptegnelseDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = tilgangsgrupper,
            daos = daos,
        )
    private val apiOppgaveService = ApiOppgaveService(
        oppgaveDao = daos.oppgaveDao,
        tilgangsgrupper = tilgangsgrupper,
        oppgaveService = oppgaveService
    )

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            daos = daos,
            versjonAvKode = "versjonAvKode",
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            tilgangsgrupper = tilgangsgrupper,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            annulleringRepository = annulleringRepository,
            env = environment,
            featureToggles = object : FeatureToggles {},
            sessionFactory = TransactionalSessionFactory(dataSource),
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
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
            featureToggles = object : FeatureToggles {}
        )


    init {
        val meldingMediator = MeldingMediator(
            sessionFactory = TransactionalSessionFactory(dataSource),
            personDao = daos.personDao,
            commandContextDao = daos.commandContextDao,
            meldingDao = daos.meldingDao,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            kommandofabrikk = kommandofabrikk,
            dokumentDao = daos.dokumentDao,
            varselRepository = VarselRepository(
                varselDao = daos.varselDao,
                definisjonDao = daos.definisjonDao
            ),
            poisonPills = PoisonPills(emptyMap()),
            env = environment,
        )
        RiverSetup(testRapid, meldingMediator, daos.meldingDuplikatkontrollDao).setUp()
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

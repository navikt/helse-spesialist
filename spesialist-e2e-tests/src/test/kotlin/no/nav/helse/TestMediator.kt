package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
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
import no.nav.helse.util.testEnv
import javax.sql.DataSource

class TestMediator(
    testRapid: TestRapid,
    dataSource: DataSource,
    environmentToggles: EnvironmentToggles,
) {
    private val daos = DBDaos(dataSource)
    private val opptegnelseDao = daos.opptegnelseDao
    private val oppgaveDao = daos.oppgaveDao
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
            reservasjonDao = daos.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = { _, _ -> false },
            tilgangsgrupper = tilgangsgrupper,
            oppgaveRepository = daos.oppgaveRepository,
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
            environmentToggles = environmentToggles,
            sessionFactory = TransactionalSessionFactory(dataSource),
            tilgangskontroll = { _, _ -> false },
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
            poisonPillDao = daos.poisonPillDao,
            ignorerMeldingerForUkjentePersoner = false
        )
        RiverSetup(
            mediator = meldingMediator,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            featureToggles = object : FeatureToggles {}
        ).registrerRivers(testRapid)
    }

    fun håndter(
        handling: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        saksbehandlerMediator.håndter(handling, saksbehandlerFraApi)
    }
}

package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
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
import no.nav.helse.modell.varsel.LegacyVarselRepository
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import javax.sql.DataSource

class TestMediator(
    testRapid: TestRapid,
    dataSource: DataSource,
) {
    private val daos = DBDaos(dataSource)
    private val opptegnelseDao = daos.opptegnelseDao
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = daos.oppgaveDao,
            reservasjonDao = daos.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            oppgaveRepository = daos.oppgaveRepository,
            tilgangsgruppehenter = { emptySet() },
        )
    private val apiOppgaveService = ApiOppgaveService(
        oppgaveDao = daos.oppgaveDao,
        oppgaveService = oppgaveService,
        sessionFactory = TransactionalSessionFactory(dataSource)
    )

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            daos = daos,
            versjonAvKode = "versjonAvKode",
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            sessionFactory = TransactionalSessionFactory(dataSource)
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
        )


    init {
        val sessionFactory = TransactionalSessionFactory(dataSource)
        val meldingMediator = MeldingMediator(
            sessionFactory = sessionFactory,
            personDao = daos.personDao,
            commandContextDao = daos.commandContextDao,
            meldingDao = daos.meldingDao,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            kommandofabrikk = kommandofabrikk,
            dokumentDao = daos.dokumentDao,
            legacyVarselRepository = LegacyVarselRepository(
                legacyVarselDao = daos.legacyVarselDao,
                definisjonDao = daos.definisjonDao
            ),
            poisonPillDao = daos.poisonPillDao,
            ignorerMeldingerForUkjentePersoner = false,
        )
        RiverSetup(
            mediator = meldingMediator,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            sessionFactory = sessionFactory,
        ).registrerRivers(testRapid)
    }

    fun håndter(
        handling: HandlingFraApi,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ) {
        saksbehandlerMediator.håndter(handling, saksbehandler, tilgangsgrupper)
    }
}

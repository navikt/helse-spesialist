package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.modell.varsel.LegacyVarselRepository
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import no.nav.helse.spesialist.domain.Saksbehandler
import javax.sql.DataSource

class TestMediator(
    testRapid: TestRapid,
    dataSource: DataSource,
    forsikringsvurderingHenter: ForsikringsvurderingHenter,
    environmentToggles: EnvironmentToggles,
) {
    private val daos = DBDaos(dataSource)
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val oppgaveService =
        OppgaveService(
            oppgaveDao = daos.oppgaveDao,
            reservasjonDao = daos.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            oppgaveRepository = daos.oppgaveRepository,
            brukerrollehenter = { Either.Success(emptySet()) },
        )
    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            daos = daos,
            versjonAvKode = "versjonAvKode",
            meldingPubliserer = meldingPubliserer,
            sessionFactory = TransactionalSessionFactory(dataSource),
        )

    private val stikkprøver =
        Stikkprøver(object : Stikkprøver.Configuration {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = false

            override fun utsFlereArbeidsgivereForlengelse() = false

            override fun selvstendigNæringsdrivendeForlengelse() = false

            override fun utsEnArbeidsgiverFørstegangsbehandling() = false

            override fun utsEnArbeidsgiverForlengelse() = false

            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false

            override fun fullRefusjonFlereArbeidsgivereForlengelse() = false

            override fun fullRefusjonEnArbeidsgiver() = false
        })

    private val kommandofabrikk =
        Kommandofabrikk(
            oppgaveService = { oppgaveService },
            subsumsjonsmelderProvider = { Subsumsjonsmelder("versjonAvKode", meldingPubliserer) },
            stikkprøver = stikkprøver,
        )

    init {
        val sessionFactory = TransactionalSessionFactory(dataSource)
        val meldingMediator =
            MeldingMediator(
                sessionFactory = sessionFactory,
                personDao = daos.personDao,
                commandContextDao = daos.commandContextDao,
                meldingDao = daos.meldingDao,
                meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
                kommandofabrikk = kommandofabrikk,
                dokumentDao = daos.dokumentDao,
                legacyVarselRepository =
                    LegacyVarselRepository(
                        legacyVarselDao = daos.legacyVarselDao,
                        definisjonDao = daos.definisjonDao,
                    ),
                poisonPillDao = daos.poisonPillDao,
                ignorerMeldingerForUkjentePersoner = false,
                versjonAvKode = "1.0.0",
            )
        RiverSetup(
            mediator = meldingMediator,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            sessionFactory = sessionFactory,
            versjonAvKode = "en_versjon",
            forsikringsvurderingHenter = forsikringsvurderingHenter,
            environmentToggles = environmentToggles,
        ).registrerRivers(testRapid)
    }

    fun håndter(
        handling: HandlingFraApi,
        saksbehandler: Saksbehandler,
    ) {
        saksbehandlerMediator.håndter(handling, saksbehandler)
    }
}

package no.nav.helse.spesialist.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.MockForsikringHenter
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture

class IntegrationTestFixture(val testRapid: TestRapid) {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val daos = inMemoryRepositoriesAndDaos.daos
    val sessionFactory = inMemoryRepositoriesAndDaos.sessionFactory
    val forsikringHenter = MockForsikringHenter()
    var forsikringToggle = false
    val environmentToggles = object : EnvironmentToggles {
        override val kanBeslutteEgneSaker: Boolean = false
        override val kanGodkjenneUtenBesluttertilgang: Boolean = false
        override val kanSeForsikring: Boolean get() = forsikringToggle
        override val devGcp: Boolean = false
    }

    init {
        KafkaModule(
            configuration = KafkaModuleTestRapidTestFixture.moduleConfiguration,
            rapidsConnection = testRapid,
            sessionFactory = sessionFactory,
            daos = daos,
            stikkprøver = object : Stikkprøver {
                override fun utsFlereArbeidsgivereFørstegangsbehandling() = false
                override fun utsFlereArbeidsgivereForlengelse() = false
                override fun selvstendigNæringsdrivendeForlengelse() = false
                override fun utsEnArbeidsgiverFørstegangsbehandling() = false
                override fun utsEnArbeidsgiverForlengelse() = false
                override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false
                override fun fullRefusjonFlereArbeidsgivereForlengelse() = false
                override fun fullRefusjonEnArbeidsgiver() = false
            },
            brukerrollehenter = { Either.Success(emptySet()) },
            forsikringHenter = forsikringHenter,
            environmentToggles = environmentToggles,
        ).also(KafkaModule::kobleOppRivers)
    }
}


package no.nav.helse.spesialist.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture

class IntegrationTestFixture(val testRapid: TestRapid) {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val daos = inMemoryRepositoriesAndDaos.daos
    val sessionFactory = inMemoryRepositoriesAndDaos.sessionFactory

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
            tilgangsgruppehenter = { Either.Success(emptySet<Tilgangsgruppe>() to emptySet()) },
        ).also(KafkaModule::kobleOppRivers)
    }
}


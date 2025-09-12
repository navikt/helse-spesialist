package no.nav.helse.spesialist.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.application.InMemoryDaos
import no.nav.helse.spesialist.application.InMemorySessionFactory
import no.nav.helse.spesialist.application.tilgangskontroll.Gruppe
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppehenter
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgrupper
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import java.util.UUID

class IntegrationTestFixture(
    val testRapid: TestRapid,
    val sessionFactory: InMemorySessionFactory = InMemorySessionFactory(),
    val daos: InMemoryDaos = InMemoryDaos(),
) {
    init {
        KafkaModule(
            configuration = KafkaModuleTestRapidTestFixture.moduleConfiguration,
            rapidsConnection = testRapid,
            sessionFactory = sessionFactory,
            daos = daos,
            tilgangsgrupper = randomTilgangsgrupper(),
            stikkprøver = object : Stikkprøver {
                override fun utsFlereArbeidsgivereFørstegangsbehandling() = false
                override fun utsFlereArbeidsgivereForlengelse() = false
                override fun utsEnArbeidsgiverFørstegangsbehandling() = false
                override fun utsEnArbeidsgiverForlengelse() = false
                override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false
                override fun fullRefusjonFlereArbeidsgivereForlengelse() = false
                override fun fullRefusjonEnArbeidsgiver() = false
            },
            tilgangsgruppehenter = object : Tilgangsgruppehenter {
                override suspend fun hentTilgangsgrupper(oid: UUID, gruppeIder: List<UUID>) = emptySet<UUID>()
                override suspend fun hentTilgangsgrupper(oid: UUID)= emptySet<Gruppe>()
            },
        ).also(KafkaModule::kobleOppRivers)
    }
}


package no.nav.helse.spesialist.kafka.testfixtures

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.kafka.KafkaModule

class KafkaModuleTestRapidTestFixture() {
    val testRapid = TestRapid()
    val moduleConfiguration = KafkaModule.Configuration(
        versjonAvKode = "versjon_1",
        ignorerMeldingerForUkjentePersoner = false,
    )
}
package no.nav.helse.spesialist.kafka.testfixtures

import no.nav.helse.spesialist.kafka.KafkaModule

object KafkaModuleTestRapidTestFixture {
    val moduleConfiguration = KafkaModule.Configuration(
        versjonAvKode = "versjon_1",
        ignorerMeldingerForUkjentePersoner = false,
    )
}

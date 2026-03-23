package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.core.instrument.Metrics
import no.nav.helse.spesialist.kafka.medRivers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class BehovtidsbrukMetrikkRiverTest {

    val rapid = TestRapid().medRivers(BehovtidsbrukMetrikkRiver())

    @Test
    fun `sanity check`() {
        rapid.sendTestMessage(
            testmelding(
                "et behov", mapOf(
                    "@final" to true,
                    "@besvart" to LocalDateTime.now(),
                    "@løsning" to emptyMap<String, Any>(),
                    "system_participating_services" to listOf(mapOf("time" to LocalDateTime.now().minusSeconds(3))),
                )
            )
        )
        assertMetrikkHarData("behov_tidsbruk")
    }

    private fun assertMetrikkHarData(metrikknavn: String) {
        assertEquals(1, Metrics.globalRegistry.get(metrikknavn).summary().count())
    }

    private fun testmelding(behov: String, innhold: Map<String, Any>) = JsonMessage.newNeed(
        behov = listOf(behov),
        map = innhold,
    ).toJson()

}



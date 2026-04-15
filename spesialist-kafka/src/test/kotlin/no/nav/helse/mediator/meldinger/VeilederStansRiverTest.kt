package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.VeilederStansRiver
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.spesialist.kafka.medRivers
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VeilederStansRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(VeilederStansRiver(mediator))

    @Test
    fun `Leser stans automatisk behandling`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.mottaMelding(any(), any()) }
    }

    @Language("JSON")
    private fun event() =
        """
    {
      "@event_name": "stans_automatisk_behandling",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "11111100000",
      "status": "STOPP_AUTOMATIKK",
      "årsaker": ["MEDISINSK_VILKAR"],
      "opprettet": "${LocalDateTime.now()}",
      "originalMelding": "{}"
    }"""
}

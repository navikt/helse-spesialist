package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class VedtakFattetRiverTest {

    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid()

    init {
        VedtakFattet.River(testRapid, mediator)
    }

    @Test
    fun `Leser inn vedtak_fattet-event`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.vedtakFattet(any(), any(), any(), any(), any()) }
    }

    @Language("JSON")
    private fun event() = """
    {
      "@event_name": "vedtak_fattet",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "aktørId": "1111100000000",
      "fødselsnummer": "11111100000",
      "organisasjonsnummer": "987654321",
      "vedtaksperiodeId": "${UUID.randomUUID()}"
    }
"""
}

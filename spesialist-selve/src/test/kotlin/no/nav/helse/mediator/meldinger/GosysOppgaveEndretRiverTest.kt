package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class GosysOppgaveEndretRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(GosysOppgaveEndretRiver(mediator))

    @Test
    fun `Leser GosysOppgaveEndret`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.mottaMelding(any<GosysOppgaveEndret>(), any()) }
    }

    @Language("JSON")
    private fun event() =
        """
    {
      "@event_name": "gosys_oppgave_endret",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "11111100000"
    }"""
}

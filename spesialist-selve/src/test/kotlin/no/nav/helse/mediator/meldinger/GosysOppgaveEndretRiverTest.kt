package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GosysOppgaveEndretRiverTest {

    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid()

    init {
        GosysOppgaveEndretRiver(testRapid, mediator)
    }

    @Test
    fun `Leser GosysOppgaveEndret`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.gosysOppgaveEndret(any(), any<GosysOppgaveEndret>(), any()) }
    }

    @Language("JSON")
    private fun event() = """
    {
      "@event_name": "gosys_oppgave_endret",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "11111100000"
    }"""

}

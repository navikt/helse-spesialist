package no.nav.helse.mediator.meldinger.påminnelser

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.medRivers
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class KommandokjedePåminnelseRiverTest {

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(KommandokjedePåminnelseRiver(mediator))

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `leser kommandokjede_påminnelse fra kafka`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.påminnelse(any(), any(), any(), any(), any()) }
    }

    @Language("JSON")
    private fun event() = """
    {
      "@event_name": "kommandokjede_påminnelse",
      "commandContextId": "${UUID.randomUUID()}",
      "meldingId": "${UUID.randomUUID()}"
    }
    """.trimIndent()
}

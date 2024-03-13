package no.nav.helse.mediator.meldinger.påminnelser

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KommandokjedePåminnelseRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<MeldingMediator>(relaxed = true)

    init {
        KommandokjedePåminnelseRiver(testRapid, mediator)
    }

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
package no.nav.helse.mediator.kafka.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingMessageTest {

    private val rapid = TestRapid()
    private val mediatorMock = mockk<SpleisbehovMediator>(relaxed = true)
    init {
        RisikovurderingMessage.Factory(rapid, mediatorMock)
    }

    @Test
    fun `tar imot risikovurdering-message`() {
        rapid.sendTestMessage("""
            {
              "@event_name": "risikovurdering",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "vedtaksperiodeId": "${UUID.randomUUID()}",
              "samletScore": 10,
              "begrunnelser": ["Detta ser ikke bra ut"],
              "ufullstendig": true
            }
        """)
        verify { mediatorMock.håndter(any(), any<RisikovurderingMessage>()) }
    }

    @Test
    fun `ignorerer ugyldig message`() {
        rapid.sendTestMessage("""
            {
              "@event_name": "risikovurdering",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "vedtaksperiodeId": "dette er ikke en UUID",
              "samletScore": 10,
              "begrunnelser": ["Detta ser ikke bra ut"],
              "ufullstendig": true
            }
        """)
        verify(exactly = 0) { mediatorMock.håndter(any(), any<RisikovurderingMessage>()) }
    }

}

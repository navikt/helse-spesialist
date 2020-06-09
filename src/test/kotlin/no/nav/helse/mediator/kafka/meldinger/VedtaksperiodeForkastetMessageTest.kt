package no.nav.helse.mediator.kafka.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeForkastetMessageTest {

    private val rapid = TestRapid()
    private val mediatorMock = mockk<SpleisbehovMediator>(relaxed = true)

    init {
        VedtaksperiodeForkastetMessage.Factory(rapid, mediatorMock)
    }

    @Test
    fun `tar imot forkastet-message`() {
        rapid.sendTestMessage(
            """
            {
              "@event_name": "vedtaksperiode_forkastet",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "vedtaksperiodeId": "${UUID.randomUUID()}",
              "fødselsnummer": "fødselsnummer"
            }
        """
        )
        verify { mediatorMock.håndter(any(), any<VedtaksperiodeForkastetMessage>()) }
    }

    @Test
    fun `ignorerer ugyldig message`() {
        rapid.sendTestMessage(
            """
            {
              "@event_name": "vedtaksperiode_forkastet",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "vedtaksperiodeId": "dette er ikke en UUID",
              "fødselsnummer": "fødselsnummer"
            }
        """
        )
        verify(exactly = 0) { mediatorMock.håndter(any(), any<VedtaksperiodeForkastetMessage>()) }
    }

}

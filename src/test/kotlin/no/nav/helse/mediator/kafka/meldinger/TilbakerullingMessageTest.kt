package no.nav.helse.mediator.kafka.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class TilbakerullingMessageTest {

    private val rapid = TestRapid()
    private val mediatorMock = mockk<HendelseMediator>(relaxed = true)
    init {
        TilbakerullingMessage.Factory(rapid, mediatorMock)
    }

    @Test
    fun `parser melding om vedtaksperiode som er slettet`() {
        rapid.sendTestMessage("""{
            "@event_name": "person_rullet_tilbake",
            "hendelseId": "030001BD-8FBA-4324-9725-D618CE5B83E9",
            "fødselsnummer": "fnr",
            "vedtaksperioderSlettet": ["A71468F7-6095-48BF-A9BB-ACF6497BEFAC", "55665958-B5A7-4E6C-BEEC-CF01D062A768"]
        }""")
        verify { mediatorMock.håndter(any<TilbakerullingMessage>()) }
    }

}

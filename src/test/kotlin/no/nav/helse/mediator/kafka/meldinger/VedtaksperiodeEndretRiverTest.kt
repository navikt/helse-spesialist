package no.nav.helse.mediator.kafka.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeEndretRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<SpleisbehovMediator>(relaxed = true)

    init {
        NyVedtaksperiodeEndretMessage.VedtaksperiodeEndretRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `tolker vedtaksperiode_endret`() {
        rapid.sendTestMessage("""{
            "@event_name": "vedtaksperiode_endret",
            "vedtaksperiodeId": "030001bd-8fba-4324-9725-d618ce5b83e9",
            "fødselsnummer": "fnr",
            "@id": "a71468f7-6095-48bf-a9bb-acf6497befac"
        }""")
        verify { mediator.håndter(any<NyVedtaksperiodeEndretMessage>()) }

    }
}

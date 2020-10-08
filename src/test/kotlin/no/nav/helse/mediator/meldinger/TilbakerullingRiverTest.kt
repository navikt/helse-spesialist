package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class TilbakerullingRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val meldingsfabrikk = Testmeldingfabrikk(FNR, "aktørid")

    init {
        NyTilbakerullingMessage.TilbakerullingRiver(testRapid, mediator)
    }

    @Test
    fun `leser tilbakerulling`() {
        val vedtaksperioderSlettet = listOf(UUID.randomUUID(), UUID.randomUUID())

        testRapid.sendTestMessage(meldingsfabrikk.lagTilbakerulling(vedtaksperioderSlettet = vedtaksperioderSlettet))
        verify(exactly = 1) { mediator.tilbakerulling(any(), any(), FNR, vedtaksperioderSlettet, any()) }
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
        clearMocks(mediator)
    }

    companion object {
        private const val FNR = "fnr"
    }
}


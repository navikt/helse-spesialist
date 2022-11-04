package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeOpprettetRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val meldingsfabrikk = Testmeldingfabrikk("fnr", "aktørid")

    init {
        VedtaksperiodeOpprettet.River(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `tolker vedtaksperiode_endret for VedtaksperiodeOpprettet`() {
        rapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(forrigeTilstand = "START"))
        verify { mediator.vedtaksperiodeOpprettet(any(), any(), any(), any(), any(), any(), any(), any()) }
    }
}

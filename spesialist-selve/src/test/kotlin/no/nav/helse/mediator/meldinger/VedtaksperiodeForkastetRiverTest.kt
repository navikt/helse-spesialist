package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val meldingsfabrikk = Testmeldingfabrikk("fnr", "aktørid")
    private val mapper = jacksonObjectMapper()

    init {
        VedtaksperiodeForkastet.VedtaksperiodeForkastetRiver(rapid, mediator)
    }

    @Test
    fun `tar imot forkastet-message`() {
        rapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet())
        verify { mediator.vedtaksperiodeForkastet(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `ignorerer ugyldig message`() {
        rapid.sendTestMessage(
            meldingsfabrikk.lagVedtaksperiodeForkastet().let { mapper.readTree(it) as ObjectNode }
                .put("vedtaksperiodeId", "dette er ikke en UUID").toString()
        )
        verify(exactly = 0) { mediator.vedtaksperiodeForkastet(any(), any(), any(), any(), any()) }
    }

}

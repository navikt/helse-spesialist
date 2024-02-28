package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import lagFødselsnummer
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val mapper = jacksonObjectMapper()

    init {
        VedtaksperiodeForkastetRiver(rapid, mediator)
    }

    @Test
    fun `tar imot forkastet-message`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        rapid.sendTestMessage(Testmeldingfabrikk.lagVedtaksperiodeForkastet("aktørId", fødselsnummer, vedtaksperiodeId))
        verify(exactly = 1) {
            mediator.vedtaksperiodeForkastet(
                hendelse = withArg {
                    assertEquals(fødselsnummer, it.fødselsnummer())
                    assertEquals(vedtaksperiodeId, it.vedtaksperiodeId())
                },
                context = any()
            )
        }
    }

    @Test
    fun `ignorerer ugyldig message`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeForkastet("aktørId", "fnr").let { mapper.readTree(it) as ObjectNode }
                .put("vedtaksperiodeId", "dette er ikke en UUID").toString()
        )
        verify(exactly = 0) { mediator.vedtaksperiodeForkastet(any(), any()) }
    }

}

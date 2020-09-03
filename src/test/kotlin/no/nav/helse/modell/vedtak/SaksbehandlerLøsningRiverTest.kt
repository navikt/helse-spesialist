package no.nav.helse.modell.vedtak

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SaksbehandlerLøsningRiverTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<IHendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        SaksbehandlerLøsning.SaksbehandlerLøsningRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser saksbehandlerløsning`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagSaksbehandlerløsning(spleisbehovId = HENDELSE, contextId = CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any<SaksbehandlerLøsning>(), any()) }
    }
}

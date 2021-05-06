package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SaksbehandlerløsningRiverTest {
    private companion object {
        private val ID = UUID.randomUUID()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<IHendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        Saksbehandlerløsning.SaksbehandlerløsningRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser saksbehandlerløsning`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagSaksbehandlerløsning(id = ID, hendelseId = GODKJENNINGSBEHOV_ID, contextId = CONTEXT))
        verify(exactly = 1) { mediator.saksbehandlerløsning(
            any(),
            ID,
            GODKJENNINGSBEHOV_ID,
            FNR,
            true,
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        ) }
    }
}

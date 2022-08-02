package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingAnnullertRiverTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        HentInfotrygdutbetalingerløsning.InfotrygdutbetalingerRiver(this, mediator)
    }

    init {
        UtbetalingAnnullert.UtbetalingAnnullertRiver(testRapid, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig UtbetalingAnnullert-melding`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagUtbetalingAnnullertEvent())
        verify(exactly = 1) { mediator.utbetalingAnnullert(any(), any()) }
    }
}

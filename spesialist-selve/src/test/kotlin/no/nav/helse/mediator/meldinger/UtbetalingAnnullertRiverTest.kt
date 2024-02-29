package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.løsninger.InfotrygdutbetalingerRiver
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingAnnullertRiverTest {
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        InfotrygdutbetalingerRiver(this, mediator)
    }

    init {
        UtbetalingAnnullertRiver(testRapid, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig UtbetalingAnnullert-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagUtbetalingAnnullert(arbeidsgiverFagsystemId = "fagsystemId"))
        verify(exactly = 1) { mediator.håndter(any<UtbetalingAnnullert>(), any()) }
    }
}

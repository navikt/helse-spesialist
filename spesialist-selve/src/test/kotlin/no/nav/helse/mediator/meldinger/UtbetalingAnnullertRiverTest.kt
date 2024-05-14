package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.InfotrygdutbetalingerRiver
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class UtbetalingAnnullertRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid =
        TestRapid().medRivers(InfotrygdutbetalingerRiver(mediator), UtbetalingAnnullertRiver(mediator))

    @Test
    fun `leser selvstendig UtbetalingAnnullert-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagUtbetalingAnnullert(arbeidsgiverFagsystemId = "fagsystemId"))
        verify(exactly = 1) { mediator.mottaMelding(any<UtbetalingAnnullert>(), any()) }
    }
}

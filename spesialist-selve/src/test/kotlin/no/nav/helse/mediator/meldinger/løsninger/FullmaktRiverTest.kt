package no.nav.helse.mediator.meldinger.løsninger

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class FullmaktRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(Fullmaktløsning.FullmaktRiver(mediator))

    @Test
    fun `motta melding med fullmakter`() {
        val fnr = lagFødselsnummer()
        val slot = slot<Fullmaktløsning>()
        testRapid.sendTestMessage(Testmeldingfabrikk.lagFullmaktløsningMedFullmakt(
            fnr,
            fom = now(),
            tom = null
        ))
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), capture(slot), any()) }
        assertTrue(slot.captured.harFullmakt)
    }

    @Test
    fun `motta melding med utdaterte fullmakter`() {
        val fnr = lagFødselsnummer()
        val slot = slot<Fullmaktløsning>()
        testRapid.sendTestMessage(Testmeldingfabrikk.lagFullmaktløsningMedFullmakt(
            fnr,
            fom = now().minusYears(2),
            tom = now().minusDays(1)
        ))
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), capture(slot), any()) }
        assertFalse(slot.captured.harFullmakt)
    }

    @Test
    fun `motta melding uten fullmakter`() {
        val fnr = lagFødselsnummer()
        val slot = slot<Fullmaktløsning>()
        testRapid.sendTestMessage(Testmeldingfabrikk.lagFullmaktløsningUtenFullmakter(fnr))
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), capture(slot), any()) }
        assertFalse(slot.captured.harFullmakt)
    }
}
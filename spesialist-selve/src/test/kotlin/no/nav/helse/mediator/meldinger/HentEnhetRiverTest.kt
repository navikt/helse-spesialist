package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetRiver
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class HentEnhetRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(HentEnhetRiver(mediator))

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig HentEnhet-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagEnhetløsning(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentEnhetløsning>(), any()) }
    }

    @Test
    fun `leser HentEnhet i et behov med flere ting`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagPersoninfoløsningComposite(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentEnhetløsning>(), any()) }
    }
}

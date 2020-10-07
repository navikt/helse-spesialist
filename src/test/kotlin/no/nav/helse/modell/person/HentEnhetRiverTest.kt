package no.nav.helse.modell.person

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class HentEnhetRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<IHendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        HentEnhetLøsning.HentEnhetRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig HentEnhet-melding`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagHentEnhetløsning(hendelseId = HENDELSE, contextId = CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentEnhetLøsning>(), any()) }
    }

    @Test
    fun `leser HentEnhet i et behov med flere ting`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagPersoninfoløsning(hendelseId = HENDELSE, contextId = CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentEnhetLøsning>(), any()) }
    }
}

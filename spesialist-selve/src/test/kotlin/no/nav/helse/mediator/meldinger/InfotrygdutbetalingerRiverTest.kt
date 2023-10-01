package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.løsninger.InfotrygdutbetalingerRiver
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdutbetalingerRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }

    private val testmeldingfabrikk = Testmeldingfabrikk()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        InfotrygdutbetalingerRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig HentInfotrygdutbetalinger-melding`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagInfotrygdutbetalingerløsning(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentPersoninfoløsning>(), any()) }
    }

    @Test
    fun `leser HentInfotrygdutbetalinger i et behov med flere ting`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagPersoninfoløsningComposite(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentPersoninfoløsning>(), any()) }
    }
}

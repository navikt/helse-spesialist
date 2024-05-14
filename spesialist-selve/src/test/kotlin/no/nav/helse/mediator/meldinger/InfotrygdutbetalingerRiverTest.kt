package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.InfotrygdutbetalingerRiver
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class InfotrygdutbetalingerRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(InfotrygdutbetalingerRiver(mediator))

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig HentInfotrygdutbetalinger-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagInfotrygdutbetalingerløsning(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentPersoninfoløsning>(), any()) }
    }

    @Test
    fun `leser HentInfotrygdutbetalinger i et behov med flere ting`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagPersoninfoløsningComposite(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentPersoninfoløsning>(), any()) }
    }
}

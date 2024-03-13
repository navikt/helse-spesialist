package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.PersoninfoRiver
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersoninfoRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        PersoninfoRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser selvstendig HentPersoninfo-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagPersoninfoløsning(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentPersoninfoløsning>(), any()) }
    }

    @Test
    fun `leser HentPersoninfo i et behov med flere ting`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagPersoninfoløsningComposite(AKTØR, FNR, HENDELSE, CONTEXT))
        verify(exactly = 1) { mediator.løsning(HENDELSE, CONTEXT, any(), any<HentPersoninfoløsning>(), any()) }
    }
}

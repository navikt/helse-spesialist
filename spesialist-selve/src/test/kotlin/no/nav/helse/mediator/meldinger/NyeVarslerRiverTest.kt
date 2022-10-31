package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test

internal class NyeVarslerRiverTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
        private const val ORGNR = "123456789"
    }

    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        NyeVarsler.NyeVarslerRiver(this, mediator)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser aktivitetslogg_ny_aktivitet`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagNyeVarsler(HENDELSE, VEDTAKSPERIODE, ORGNR))
        verify(exactly = 1) { mediator.nyeVarsler(any(), any(), any(), any(), any()) }
    }
}
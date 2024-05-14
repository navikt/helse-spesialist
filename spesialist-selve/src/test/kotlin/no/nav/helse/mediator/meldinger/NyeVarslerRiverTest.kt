package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class NyeVarslerRiverTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val ORGNR = "123456789"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(NyeVarslerRiver(mediator))

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser aktivitetslogg_ny_aktivitet`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagAktivitetsloggNyAktivitet(FNR, HENDELSE, VEDTAKSPERIODE, ORGNR))
        verify(exactly = 1) { mediator.mottaMelding(any<NyeVarsler>(), any()) }
    }

    @Test
    fun `leser nye_varsler`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagNyeVarsler(FNR, HENDELSE, VEDTAKSPERIODE, ORGNR))
        verify(exactly = 1) { mediator.mottaMelding(any<NyeVarsler>(), any()) }
    }
}

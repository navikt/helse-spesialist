package no.nav.helse.mediator.kafka.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class GodkjenningMessageRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
        private const val ORGNR = "123456789"
        private val FOM = LocalDate.of(2020, 1, 1)
        private val TOM = LocalDate.of(2020, 1, 31)
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        NyGodkjenningMessage.GodkjenningMessageRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser Godkjenningbehov`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagGodkjenningsbehov(id = HENDELSE, vedtaksperiodeId = VEDTAKSPERIODE, organisasjonsnummer = ORGNR, periodeFom = FOM, periodeTom = TOM))
        verify(exactly = 1) { mediator.godkjenning(any(), HENDELSE, FNR, AKTØR, ORGNR, FOM, TOM, VEDTAKSPERIODE, emptyList(), null, any()) }
    }
}

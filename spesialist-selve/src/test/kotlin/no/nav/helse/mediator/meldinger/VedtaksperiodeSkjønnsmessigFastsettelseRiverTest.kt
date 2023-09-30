package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeSkjønnsmessigFastsettelseRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val meldingsfabrikk = Testmeldingfabrikk()

    init {
        VedtaksperiodeSkjønnsmessigFastsettelseRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `tolker vedtaksperiode_endret for VedtaksperiodeSkjønnsmessigFastsetting`() {
        rapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(aktørId = "aktørId", fødselsnummer = "fnr", gjeldendeTilstand = "AVVENTER_SKJØNNSMESSIG_FASTSETTELSE"))
        verify { mediator.vedtaksperiodeSkjønnsmessigFastsettelse(any(), any(), any(), any(), any(), any(), any()) }
    }
}

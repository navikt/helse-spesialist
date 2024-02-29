package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeEndretRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)

    init {
        VedtaksperiodeEndretRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `leser vedtaksperiode_endret`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeEndret(
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER
            )
        )
        verify(exactly = 1) { mediator.vedtaksperiodeEndret(any(), any()) }
    }

    @Test
    fun `leser ikke vedtaksperiode_endret der forrigeTilstand=START`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeEndret(
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                forrigeTilstand = "START"
            )
        )
        verify(exactly = 0) { mediator.vedtaksperiodeEndret(any(), any()) }
    }
}

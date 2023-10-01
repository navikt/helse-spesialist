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

internal class VedtaksperiodeOpprettetRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)

    init {
        VedtaksperiodeOpprettetRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `tolker vedtaksperiode_endret for VedtaksperiodeOpprettet`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeOpprettet(aktørId = AKTØR, fødselsnummer = FØDSELSNUMMER)
        )
        verify(exactly = 1) { mediator.vedtaksperiodeOpprettet(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        ) }
    }
}

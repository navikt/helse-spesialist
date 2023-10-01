package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeNyUtbetalingRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testmeldingfabrikk = Testmeldingfabrikk()

    init {
        VedtaksperiodeNyUtbetalingRiver(testRapid, mediator)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser inn vedtaksperiode_ny_utbetaling`() {
        val hendelseId = UUID.randomUUID()
        testRapid.sendTestMessage(
            testmeldingfabrikk.lagVedtaksperiodeNyUtbetaling(
                id = hendelseId,
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
            )
        )
        verify(exactly = 1) { mediator.vedtaksperiodeNyUtbetaling(FØDSELSNUMMER, hendelseId, VEDTAKSPERIODE_ID, UTBETALING_ID, any(), any()) }
    }
}

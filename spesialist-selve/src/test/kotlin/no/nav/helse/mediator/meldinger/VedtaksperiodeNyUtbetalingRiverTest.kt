package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeNyUtbetalingRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<MeldingMediator>(relaxed = true)

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
            Testmeldingfabrikk.lagVedtaksperiodeNyUtbetaling(
                id = hendelseId,
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
            )
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding = withArg<VedtaksperiodeNyUtbetaling> {
                    assertEquals(hendelseId, it.id)
                    assertEquals(FØDSELSNUMMER, it.fødselsnummer())
                    assertEquals(VEDTAKSPERIODE_ID, it.vedtaksperiodeId())
                    assertEquals(UTBETALING_ID, it.utbetalingId)
                },
                messageContext = any()
            )
        }
    }
}

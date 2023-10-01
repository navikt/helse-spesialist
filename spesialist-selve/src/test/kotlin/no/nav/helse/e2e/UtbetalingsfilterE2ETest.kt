package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import org.junit.jupiter.api.Test

internal class UtbetalingsfilterE2ETest : AbstractE2ETest() {

    @Test
    fun `Går gjennom begge filtreringer`() {
        fremForbiUtbetalingsfilter(
            periodetype = FORLENGELSE,
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovIkkeBesvart()
        assertIkkeAvvistIUtbetalingsfilter()
    }

    @Test
    fun `går gjennom uten personutbetaling`() {
        fremForbiUtbetalingsfilter(
            periodetype = FORLENGELSE,
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertIkkeAvvistIUtbetalingsfilter()
    }

    // Dette er litt skjørt, men jeg finner ikke noen bedre måte å asserte at UtbetalingfilterCommand kjørte OK på
    private fun assertIkkeAvvistIUtbetalingsfilter() = assertSisteEtterspurteBehov("EgenAnsatt")

    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
    }
}

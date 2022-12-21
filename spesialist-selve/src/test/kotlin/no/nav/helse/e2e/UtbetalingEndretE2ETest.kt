package no.nav.helse.e2e

import AbstractE2ETestV2
import org.junit.jupiter.api.Test

internal class UtbetalingEndretE2ETest: AbstractE2ETestV2() {
    @Test
    fun `Lagrer personbeløp og arbeidsgiverbeløp ved innlesing av utbetaling_endret`() {
        håndterSøknad()
        håndterVedtaksperiodeNyUtbetaling()
        håndterUtbetalingOpprettet(arbeidsgiverbeløp = 20000, personbeløp = 20000)
        assertUtbetaling(20000, 20000)
    }
}
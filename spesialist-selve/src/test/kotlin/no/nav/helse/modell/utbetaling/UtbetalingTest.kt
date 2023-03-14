package no.nav.helse.modell.utbetaling

import java.util.UUID
import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.FULL_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_UTBETALING
import no.nav.helse.modell.utbetaling.Refusjonstype.NEGATIVT_BELØP
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UtbetalingTest {
    @Test
    fun `ingen utbetaling`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = 0, personbeløp = 0)
        assertEquals(INGEN_UTBETALING, utbetaling.refusjonstype())
    }
    @Test
    fun `ingen refusjon`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = 0, personbeløp = 1)
        assertEquals(INGEN_REFUSJON, utbetaling.refusjonstype())
    }
    @Test
    fun `full refusjon`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = 1, personbeløp = 0)
        assertEquals(FULL_REFUSJON, utbetaling.refusjonstype())
    }
    @Test
    fun `delvis refusjon`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = 1, personbeløp = 1)
        assertEquals(DELVIS_REFUSJON, utbetaling.refusjonstype())
    }
    @Test
    fun `negativt beløp begge`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = -1, personbeløp = -1)
        assertEquals(NEGATIVT_BELØP, utbetaling.refusjonstype())
    }
    @Test
    fun `negativt beløp arbeidsgiver`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = -1, personbeløp = 0)
        assertEquals(NEGATIVT_BELØP, utbetaling.refusjonstype())
    }
    @Test
    fun `negativt beløp person`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = 0, personbeløp = -1)
        assertEquals(NEGATIVT_BELØP, utbetaling.refusjonstype())
    }
    @Test
    fun `negativt beløp arbeidsgiver, positivt beløp person`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = -1, personbeløp = 1)
        assertEquals(NEGATIVT_BELØP, utbetaling.refusjonstype())
    }
    @Test
    fun `positivt beløp arbeidsgiver, negativt beløp person`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), arbeidsgiverbeløp = 1, personbeløp = -1)
        assertEquals(NEGATIVT_BELØP, utbetaling.refusjonstype())
    }
}
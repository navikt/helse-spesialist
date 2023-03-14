package no.nav.helse.modell.utbetaling

import java.util.UUID
import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.FULL_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_REFUSJON
import no.nav.helse.modell.utbetaling.Refusjonstype.INGEN_UTBETALING
import no.nav.helse.modell.utbetaling.Refusjonstype.NEGATIVT_BELØP
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    @Test
    fun `referential equals`() {
        val utbetaling = Utbetaling(UUID.randomUUID(), 0, 0)
        assertEquals(utbetaling, utbetaling)
    }

    @Test
    fun `structural equals`() {
        val utbetalingId = UUID.randomUUID()
        val utbetaling1 = Utbetaling(utbetalingId, 0, 0)
        val utbetaling2 = Utbetaling(utbetalingId, 0, 0)
        assertEquals(utbetaling1, utbetaling2)
        assertEquals(utbetaling1.hashCode(), utbetaling2.hashCode())
    }

    @Test
    fun `not equals - utbetalingId`() {
        val utbetaling1 = Utbetaling(UUID.randomUUID(), 0, 0)
        val utbetaling2 = Utbetaling(UUID.randomUUID(), 0, 0)
        assertNotEquals(utbetaling1, utbetaling2)
        assertNotEquals(utbetaling1.hashCode(), utbetaling2.hashCode())
    }

    @Test
    fun `not equals - arbeidsgiverbeløp`() {
        val utbetalingId = UUID.randomUUID()
        val utbetaling1 = Utbetaling(utbetalingId, 1, 0)
        val utbetaling2 = Utbetaling(utbetalingId, 0, 0)
        assertNotEquals(utbetaling1, utbetaling2)
        assertNotEquals(utbetaling1.hashCode(), utbetaling2.hashCode())
    }

    @Test
    fun `not equals - personbeløp`() {
        val utbetalingId = UUID.randomUUID()
        val utbetaling1 = Utbetaling(utbetalingId, 0, 1)
        val utbetaling2 = Utbetaling(utbetalingId, 0, 0)
        assertNotEquals(utbetaling1, utbetaling2)
        assertNotEquals(utbetaling1.hashCode(), utbetaling2.hashCode())
    }
}
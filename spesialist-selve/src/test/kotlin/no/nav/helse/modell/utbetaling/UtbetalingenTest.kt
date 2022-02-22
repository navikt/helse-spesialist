package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.person.PersonDao.Utbetalingen
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.bareUtbetalingTilSykmeldt
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.delvisRefusjon
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilArbeidsgiver
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.utbetalingTilSykmeldt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingenTest {

    @Test
    fun `ingen utbetaling`() {
        assertIngenUtbetaling(null)
        assertIngenUtbetaling(utbetalingen(null, null))
        assertIngenUtbetaling(utbetalingen(0, 0))
        assertIngenUtbetaling(utbetalingen(0, null))
        assertIngenUtbetaling(utbetalingen(null, 0))
    }

    @Test
    fun `delvis refusjon`() {
        assertDelvisRefusjon(utbetalingen(1,1))
        assertDelvisRefusjon(utbetalingen(-1, 1))
        assertDelvisRefusjon(utbetalingen(1, -1))
        assertDelvisRefusjon(utbetalingen(-1, -1))
    }

    @Test
    fun `full refusjon`() {
        assertFullRefusjon(utbetalingen(0,1))
        assertFullRefusjon(utbetalingen(0,-1))
        assertFullRefusjon(utbetalingen(null, 1))
        assertFullRefusjon(utbetalingen(null, -1))
    }

    @Test
    fun `ingen refusjon`() {
        assertIngenRefusjon(utbetalingen(1,0))
        assertIngenRefusjon(utbetalingen(-1,0))
        assertIngenRefusjon(utbetalingen(1, null))
        assertIngenRefusjon(utbetalingen(-1, null))
    }

    private fun utbetalingen(personNettoBeløp: Int?, arbeidsgiverNettoBeløp: Int?) = Utbetalingen(
        utbetalingId = null,
        personNettoBeløp = personNettoBeløp,
        arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp
    )

    private fun assertIngenUtbetaling(utbetalingen: Utbetalingen?) {
        assertFalse(utbetalingen.utbetalingTilArbeidsgiver())
        assertFalse(utbetalingen.utbetalingTilSykmeldt())
        assertFalse(utbetalingen.delvisRefusjon())
        assertFalse(utbetalingen.bareUtbetalingTilSykmeldt())
        assertTidligereLogikk(utbetalingen)
    }

    private fun assertDelvisRefusjon(utbetalingen: Utbetalingen) {
        assertTrue(utbetalingen.utbetalingTilArbeidsgiver())
        assertTrue(utbetalingen.utbetalingTilSykmeldt())
        assertTrue(utbetalingen.delvisRefusjon())
        assertFalse(utbetalingen.bareUtbetalingTilSykmeldt())
        assertTidligereLogikk(utbetalingen)
    }

    private fun assertFullRefusjon(utbetalingen: Utbetalingen) {
        assertTrue(utbetalingen.utbetalingTilArbeidsgiver())
        assertFalse(utbetalingen.utbetalingTilSykmeldt())
        assertFalse(utbetalingen.delvisRefusjon())
        assertFalse(utbetalingen.bareUtbetalingTilSykmeldt())
        assertTidligereLogikk(utbetalingen)
    }

    private fun assertIngenRefusjon(utbetalingen: Utbetalingen) {
        assertFalse(utbetalingen.utbetalingTilArbeidsgiver())
        assertTrue(utbetalingen.utbetalingTilSykmeldt())
        assertFalse(utbetalingen.delvisRefusjon())
        assertTrue(utbetalingen.bareUtbetalingTilSykmeldt())
        assertTidligereLogikk(utbetalingen)
    }

    private fun assertTidligereLogikk(utbetalingen: Utbetalingen?) {
        val (bareUtbetalingTilSykmeldt, delvisRefusjon) = tidligereLogikk(utbetalingen)
        assertEquals(bareUtbetalingTilSykmeldt, utbetalingen.bareUtbetalingTilSykmeldt())
        assertEquals(delvisRefusjon, utbetalingen.delvisRefusjon())
    }

    private fun tidligereLogikk(vedtaksperiodensUtbetaling: Utbetalingen?) : Pair<Boolean, Boolean> {
        val bareUtbetalingTilSykmeldt =
            (vedtaksperiodensUtbetaling?.personNettoBeløp ?: 0) != 0 && (vedtaksperiodensUtbetaling?.arbeidsgiverNettoBeløp ?: 0) == 0
        val delvisRefusjon = (vedtaksperiodensUtbetaling?.personNettoBeløp ?: 0) != 0 && (vedtaksperiodensUtbetaling?.arbeidsgiverNettoBeløp ?: 0) != 0
        return bareUtbetalingTilSykmeldt to delvisRefusjon
    }
}

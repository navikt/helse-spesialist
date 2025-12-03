package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.UtbetalingTag.Companion.deltUtbetaling
import no.nav.helse.spesialist.domain.UtbetalingTag.Companion.inneholderUtbetalingTilSykmeldt
import no.nav.helse.spesialist.domain.UtbetalingTag.Companion.kunUtbetalingTilArbeidsgiver
import no.nav.helse.spesialist.domain.UtbetalingTag.Companion.kunUtbetalingTilSykmeldt
import no.nav.helse.spesialist.domain.UtbetalingTag.Companion.trekkesPenger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtbetalingTagTest {
    @Test
    fun `ingen utbetaling`() {
        val tags = listOf("IngenUtbetaling")
        assertFalse(tags.trekkesPenger())
        assertFalse(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertFalse(tags.deltUtbetaling())
    }

    @Test
    fun `utbetaling til arbeidsgiver`() {
        val tags = listOf("Arbeidsgiverutbetaling")
        assertFalse(tags.trekkesPenger())
        assertFalse(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertTrue(tags.kunUtbetalingTilArbeidsgiver())
        assertFalse(tags.deltUtbetaling())
    }

    @Test
    fun `negativ utbetaling til arbeidsgiver`() {
        val tags = listOf("NegativArbeidsgiverutbetaling")
        assertTrue(tags.trekkesPenger())
        assertFalse(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertTrue(tags.kunUtbetalingTilArbeidsgiver())
        assertFalse(tags.deltUtbetaling())
    }

    @Test
    fun `utbetaling til person`() {
        val tags = listOf("Personutbetaling")
        assertFalse(tags.trekkesPenger())
        assertTrue(tags.inneholderUtbetalingTilSykmeldt())
        assertTrue(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertFalse(tags.deltUtbetaling())
    }

    @Test
    fun `negativ utbetaling til person`() {
        val tags = listOf("NegativPersonutbetaling")
        assertTrue(tags.trekkesPenger())
        assertTrue(tags.inneholderUtbetalingTilSykmeldt())
        assertTrue(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertFalse(tags.deltUtbetaling())
    }

    @Test
    fun `utbetaling til arbeidsgiver og person`() {
        val tags = listOf("Personutbetaling", "Arbeidsgiverutbetaling")
        assertFalse(tags.trekkesPenger())
        assertTrue(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertTrue(tags.deltUtbetaling())
    }

    @Test
    fun `negativ utbetaling til arbeidsgiver og person`() {
        val tags = listOf("NegativPersonutbetaling", "NegativArbeidsgiverutbetaling")
        assertTrue(tags.trekkesPenger())
        assertTrue(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertTrue(tags.deltUtbetaling())
    }

    @Test
    fun `negativ utbetaling til arbeidsgiver og og positiv utbetaling til person`() {
        val tags = listOf("Personutbetaling", "NegativArbeidsgiverutbetaling")
        assertTrue(tags.trekkesPenger())
        assertTrue(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertTrue(tags.deltUtbetaling())
    }

    @Test
    fun `positiv utbetaling til arbeidsgiver og og negativ utbetaling til person`() {
        val tags = listOf("NegativPersonutbetaling", "Arbeidsgiverutbetaling")
        assertTrue(tags.trekkesPenger())
        assertTrue(tags.inneholderUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilSykmeldt())
        assertFalse(tags.kunUtbetalingTilArbeidsgiver())
        assertTrue(tags.deltUtbetaling())
    }
}

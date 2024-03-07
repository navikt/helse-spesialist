package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.Periode.Companion.til
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PeriodeTest {

    @Test
    fun `fom etter tom`() {
        assertThrows<IllegalArgumentException> {
            Periode(2.januar, 1.januar)
        }
    }

    @Test
    fun til() {
        assertEquals(Periode(1.januar, 2.januar), 1.januar til 2.januar)
    }

    @Test
    fun `overlapper delvis`() {
        val periode1 = Periode(1.januar, 31.januar)
        val periode2 = Periode(15.januar, 15.februar)
        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapper helt`() {
        val periode1 = Periode(1.januar, 31.januar)
        val periode2 = Periode(2.januar, 2.januar)
        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapper nøyaktig`() {
        val periode1 = Periode(1.januar, 31.januar)
        val periode2 = Periode(1.januar, 31.januar)
        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapper ikke`() {
        val periode1 = Periode(2.januar, 31.januar)
        val periode2 = Periode(2.februar, 2.februar)
        val periode3 = Periode(1.januar, 1.januar)
        assertFalse(periode1.overlapperMed(periode2))
        assertFalse(periode1.overlapperMed(periode3))
        assertFalse(periode2.overlapperMed(periode1))
        assertFalse(periode2.overlapperMed(periode3))
        assertFalse(periode3.overlapperMed(periode1))
        assertFalse(periode3.overlapperMed(periode2))
    }

    @Test
    fun `referential equals`() {
        val periode = Periode(1.januar, 3.januar)
        assertEquals(periode, periode)
        assertEquals(periode.hashCode(), periode.hashCode())
    }

    @Test
    fun `structural equals`() {
        assertEquals(Periode(1.januar, 3.januar), Periode(1.januar, 3.januar))
        assertEquals(Periode(1.januar, 3.januar).hashCode(), Periode(1.januar, 3.januar).hashCode())
    }

    @Test
    fun `not equals`() {
        assertNotEquals(Periode(2.januar, 3.januar), Periode(1.januar, 3.januar))
        assertNotEquals(Periode(1.januar, 2.januar), Periode(1.januar, 3.januar))
        assertNotEquals(Periode(2.januar, 4.januar), Periode(1.januar, 3.januar))
        assertNotEquals(Periode(2.januar, 3.januar).hashCode(), Periode(1.januar, 3.januar).hashCode())
        assertNotEquals(Periode(1.januar, 2.januar).hashCode(), Periode(1.januar, 3.januar).hashCode())
        assertNotEquals(Periode(2.januar, 4.januar).hashCode(), Periode(1.januar, 3.januar).hashCode())
    }
}
package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.Periode.Companion.til
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
package no.nav.helse.spesialist.application.modell

import no.nav.helse.modell.person.vedtaksperiode.Periode
import no.nav.helse.modell.person.vedtaksperiode.Periode.Companion.til
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
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
            Periode(2 jan 2018, 1 jan 2018)
        }
    }

    @Test
    fun til() {
        assertEquals(Periode(1 jan 2018, 2 jan 2018), 1 jan 2018 til (2 jan 2018))
    }

    @Test
    fun `overlapper delvis`() {
        val periode1 = Periode(1 jan 2018, 31 jan 2018)
        val periode2 = Periode(15 jan 2018, 15 feb 2018)
        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapper helt`() {
        val periode1 = Periode(1 jan 2018, 31 jan 2018)
        val periode2 = Periode(2 jan 2018, 2 jan 2018)
        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapper nøyaktig`() {
        val periode1 = Periode(1 jan 2018, 31 jan 2018)
        val periode2 = Periode(1 jan 2018, 31 jan 2018)
        assertTrue(periode1.overlapperMed(periode2))
        assertTrue(periode2.overlapperMed(periode1))
    }

    @Test
    fun `overlapper ikke`() {
        val periode1 = Periode(2 jan 2018, 31 jan 2018)
        val periode2 = Periode(2 feb 2018, 2 feb 2018)
        val periode3 = Periode(1 jan 2018, 1 jan 2018)
        assertFalse(periode1.overlapperMed(periode2))
        assertFalse(periode1.overlapperMed(periode3))
        assertFalse(periode2.overlapperMed(periode1))
        assertFalse(periode2.overlapperMed(periode3))
        assertFalse(periode3.overlapperMed(periode1))
        assertFalse(periode3.overlapperMed(periode2))
    }

    @Test
    fun `referential equals`() {
        val periode = Periode(1 jan 2018, 3 jan 2018)
        assertEquals(periode, periode)
        assertEquals(periode.hashCode(), periode.hashCode())
    }

    @Test
    fun `structural equals`() {
        assertEquals(Periode(1 jan 2018, 3 jan 2018), Periode(1 jan 2018, 3 jan 2018))
        assertEquals(Periode(1 jan 2018, 3 jan 2018).hashCode(), Periode(1 jan 2018, 3 jan 2018).hashCode())
    }

    @Test
    fun `not equals`() {
        assertNotEquals(Periode(2 jan 2018, 3 jan 2018), Periode(1 jan 2018, 3 jan 2018))
        assertNotEquals(Periode(1 jan 2018, 2 jan 2018), Periode(1 jan 2018, 3 jan 2018))
        assertNotEquals(Periode(2 jan 2018, 4 jan 2018), Periode(1 jan 2018, 3 jan 2018))
        assertNotEquals(Periode(2 jan 2018, 3 jan 2018).hashCode(), Periode(1 jan 2018, 3 jan 2018).hashCode())
        assertNotEquals(Periode(1 jan 2018, 2 jan 2018).hashCode(), Periode(1 jan 2018, 3 jan 2018).hashCode())
        assertNotEquals(Periode(2 jan 2018, 4 jan 2018).hashCode(), Periode(1 jan 2018, 3 jan 2018).hashCode())
    }
}

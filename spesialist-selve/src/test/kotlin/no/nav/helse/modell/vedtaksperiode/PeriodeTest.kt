package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class PeriodeTest {

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
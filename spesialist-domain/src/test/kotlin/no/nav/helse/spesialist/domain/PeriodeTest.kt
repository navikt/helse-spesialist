package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.Periode.Companion.til
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun dager() {
        val periode = Periode(1 jan 2018, 5 jan 2018)
        assertEquals(listOf(1 jan 2018, 2 jan 2018, 3 jan 2018, 4 jan 2018, 5 jan 2018), periode.datoer())
    }

    @Test
    fun `forlenges av`() {
        val periode = Periode(2 jan 2018, 5 jan 2018)
        assertTrue(periode.forlengesAv(6 jan 2018))
        assertFalse(periode.forlengesAv(7 jan 2018))
        assertFalse(periode.forlengesAv(5 jan 2018))
        assertFalse(periode.forlengesAv(1 jan 2018))
    }

    @Test
    fun `dager til perioder`() {
        val dager = listOf(
            1 jan 2018, 2 jan 2018, 3 jan 2018, 4 jan 2018,
            8 jan 2018, 9 jan 2018,
            11 jan 2018,
            13 jan 2018, 14 jan 2018, 15 jan 2018, 16 jan 2018,
            30 jan 2018, 31 jan 2018, 1 feb 2018
        )
        assertEquals(
            listOf(
                Periode(1 jan 2018, 4 jan 2018),
                Periode(8 jan 2018, 9 jan 2018),
                Periode(11 jan 2018, 11 jan 2018),
                Periode(13 jan 2018, 16 jan 2018),
                Periode(30 jan 2018, 1 feb 2018),
            ),
            dager.tilPerioder()
        )
    }
}

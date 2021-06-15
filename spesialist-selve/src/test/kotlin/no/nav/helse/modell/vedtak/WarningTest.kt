package no.nav.helse.modell.vedtak

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class WarningTest {

    @Test
    fun `kan ikke lagre tom eller blank melding`() {
        assertFalse(Warning("", WarningKilde.Spleis).kanLagres())
        assertFalse(Warning("     ", WarningKilde.Spleis).kanLagres())
    }

    @Test
    fun `kan lagre melding med tekstlig innhold`() {
        assertTrue(Warning("Warning", WarningKilde.Spleis).kanLagres())
    }

    @Test
    fun `henter ut meldinger`() {
        val melding1 = "Warning A"
        val melding2 = "Warning B"
        assertEquals(listOf(melding1, melding2), Warning.meldinger(listOf(Warning(melding1, WarningKilde.Spleis), Warning(melding2, WarningKilde.Spleis))))
    }

    @Test
    fun likhet() {
        val warning1 = Warning("Warning A", WarningKilde.Spleis)
        val warning2 = Warning("Warning A", WarningKilde.Spleis)
        val warning3 = Warning("Warning A", WarningKilde.Spesialist)
        val warning4 = Warning("Warning B", WarningKilde.Spleis)
        assertEquals(warning1, warning1)
        assertEquals(warning1, warning2)
        assertNotEquals(warning1, warning3)
        assertNotEquals(warning1, warning4)
    }
}

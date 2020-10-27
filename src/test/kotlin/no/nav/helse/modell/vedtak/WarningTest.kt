package no.nav.helse.modell.vedtak

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.WarningDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WarningTest {
    private companion object {
        private const val VEDTAK_REF = 1L
    }

    private val warningDao = mockk<WarningDao>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(warningDao)
    }

    @Test
    fun `lagrer ikke tom melding`() {
        Warning("", WarningKilde.Spleis).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAK_REF, any(), any()) }
    }

    @Test
    fun `lagrer ikke blank melding`() {
        Warning("     ", WarningKilde.Spleis).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAK_REF, any(), any()) }
    }

    @Test
    fun `lagrer melding`() {
        val melding = "Warning"
        val kilde = WarningKilde.Spleis
        Warning(melding, kilde).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAK_REF, melding, kilde) }
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

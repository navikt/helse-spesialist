package no.nav.helse.modell.vedtak

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.WarningDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WarningDtoTest {
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
        WarningDto("", WarningKilde.Spleis).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAK_REF, any(), any()) }
    }

    @Test
    fun `lagrer ikke blank melding`() {
        WarningDto("     ", WarningKilde.Spleis).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAK_REF, any(), any()) }
    }

    @Test
    fun `lagrer melding`() {
        val melding = "Warning"
        val kilde = WarningKilde.Spleis
        WarningDto(melding, kilde).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAK_REF, melding, kilde) }
    }

    @Test
    fun `henter ut meldinger`() {
        val melding1 = "Warning A"
        val melding2 = "Warning B"
        assertEquals(listOf(melding1, melding2), WarningDto.meldinger(listOf(WarningDto(melding1, WarningKilde.Spleis), WarningDto(melding2, WarningKilde.Spleis))))
    }

    @Test
    fun likhet() {
        val warning1 = WarningDto("Warning A", WarningKilde.Spleis)
        val warning2 = WarningDto("Warning A", WarningKilde.Spleis)
        val warning3 = WarningDto("Warning A", WarningKilde.Spesialist)
        val warning4 = WarningDto("Warning B", WarningKilde.Spleis)
        assertEquals(warning1, warning1)
        assertEquals(warning1, warning2)
        assertNotEquals(warning1, warning3)
        assertNotEquals(warning1, warning4)
    }
}

package no.nav.helse.modell.vedtak

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
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
        Warning("", WarningKilde.Spleis, LocalDateTime.now()).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAK_REF, any(), any(), any()) }
    }

    @Test
    fun `lagrer ikke blank melding`() {
        Warning("     ", WarningKilde.Spleis, LocalDateTime.now()).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAK_REF, any(), any(), any()) }
    }

    @Test
    fun `lagrer melding`() {
        val melding = "Warning"
        val kilde = WarningKilde.Spleis
        val opprettet = LocalDateTime.now()
        Warning(melding, kilde, opprettet).lagre(warningDao, VEDTAK_REF)
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAK_REF, melding, kilde, opprettet) }
    }

    @Test
    fun likhet() {
        val warning1 = Warning("Warning A", WarningKilde.Spleis, LocalDateTime.now())
        val warning2 = Warning("Warning A", WarningKilde.Spleis, LocalDateTime.now())
        val warning3 = Warning("Warning A", WarningKilde.Spesialist, LocalDateTime.now())
        val warning4 = Warning("Warning B", WarningKilde.Spleis, LocalDateTime.now())
        assertEquals(warning1, warning1)
        assertEquals(warning1, warning2)
        assertNotEquals(warning1, warning3)
        assertNotEquals(warning1, warning4)
    }
}

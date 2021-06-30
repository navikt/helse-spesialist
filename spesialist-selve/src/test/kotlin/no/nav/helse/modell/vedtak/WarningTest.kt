package no.nav.helse.modell.vedtak

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class WarningTest {

    @Test
    fun `tomme advarsler er en egen, no-op type`() {
        assertTrue { Warning.warning("", WarningKilde.Spleis) is EmptyWarning }
        assertTrue { Warning.warning("     ", WarningKilde.Spleis) is EmptyWarning }
        assertTrue { Warning.warning("\t", WarningKilde.Spleis) is EmptyWarning }
        assertTrue { Warning.warning("\n", WarningKilde.Spleis) is EmptyWarning }
        assertTrue { Warning.warning("\r\n", WarningKilde.Spleis) is EmptyWarning }
        assertTrue { Warning.warning("${Typography.nbsp}", WarningKilde.Spleis) is EmptyWarning }
        assertTrue { Warning.warning("\r\n.\t", WarningKilde.Spleis) is ActualWarning }
        assertTrue { Warning.warning("Warning", WarningKilde.Spleis) is ActualWarning }
    }

    @Test
    fun `henter ut meldinger`() {
        val melding1 = "Warning A"
        val melding2 = "Warning B"
        assertEquals(listOf(melding1, melding2), Warning.meldinger(listOf(Warning.warning(melding1, WarningKilde.Spleis), Warning.warning(melding2, WarningKilde.Spleis))))
    }

    @Test
    fun likhet() {
        val warning1 = Warning.warning("Warning A", WarningKilde.Spleis)
        val warning2 = Warning.warning("Warning A", WarningKilde.Spleis)
        val warning3 = Warning.warning("Warning A", WarningKilde.Spesialist)
        val warning4 = Warning.warning("Warning B", WarningKilde.Spleis)
        assertEquals(warning1, warning1)
        assertEquals(warning1, warning2)
        assertNotEquals(warning1, warning3)
        assertNotEquals(warning1, warning4)
    }
}

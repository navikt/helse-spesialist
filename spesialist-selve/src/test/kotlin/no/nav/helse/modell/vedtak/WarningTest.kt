package no.nav.helse.modell.vedtak

import no.nav.helse.modell.vedtak.Warning.Companion.warning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class WarningTest {

    @Test
    fun `tomme advarsler er en egen, no-op type`() {
        assertTrue { warning("", WarningKilde.Spleis) is NotAWarning }
        assertTrue { warning("     ", WarningKilde.Spleis) is NotAWarning }
        assertTrue { warning("\t", WarningKilde.Spleis) is NotAWarning }
        assertTrue { warning("\n", WarningKilde.Spleis) is NotAWarning }
        assertTrue { warning("\r\n", WarningKilde.Spleis) is NotAWarning }
        assertTrue { warning("${Typography.nbsp}", WarningKilde.Spleis) is NotAWarning }
        assertTrue { warning("\r\n.\t", WarningKilde.Spleis) is Warning }
        assertTrue { warning("Warning", WarningKilde.Spleis) is Warning }
    }

    @Test
    fun `henter ut meldinger`() {
        val melding1 = "Warning A"
        val melding2 = "Warning B"
        assertEquals(listOf(melding1, melding2), Warning.meldinger(listOf(warning(melding1, WarningKilde.Spleis), warning(melding2, WarningKilde.Spleis))))
    }

    @Test
    fun likhet() {
        val warning1 = warning("Warning A", WarningKilde.Spleis)
        val warning2 = warning("Warning A", WarningKilde.Spleis)
        val warning3 = warning("Warning A", WarningKilde.Spesialist)
        val warning4 = warning("Warning B", WarningKilde.Spleis)
        assertEquals(warning1, warning1)
        assertEquals(warning1, warning2)
        assertNotEquals(warning1, warning3)
        assertNotEquals(warning1, warning4)
    }
}

package no.nav.helse.modell

import DatabaseIntegrationTest
import java.time.LocalDateTime
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class WarningDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `lagrer og leser warnings`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val testwarnings = listOf(
            Warning("Warning A", WarningKilde.Spleis, LocalDateTime.now()),
            Warning("Warning B", WarningKilde.Spleis, LocalDateTime.now())
        )
        val testwarning = Warning("Warning C", WarningKilde.Spesialist, LocalDateTime.now())
        warningDao.leggTilWarnings(VEDTAKSPERIODE, testwarnings)
        warningDao.leggTilWarning(VEDTAKSPERIODE, testwarning)
        assertWarnings(testwarnings + listOf(testwarning), warningDao.finnWarnings(VEDTAKSPERIODE))
    }

    @Test
    fun `sletter ikke gamle warnings`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val testwarnings1 = listOf(
            Warning("Warning A", WarningKilde.Spleis, LocalDateTime.now()),
            Warning("Warning B", WarningKilde.Spleis, LocalDateTime.now())
        )
        warningDao.leggTilWarnings(VEDTAKSPERIODE, testwarnings1)
        val testwarnings2 = listOf(
            Warning("Warning C", WarningKilde.Spleis, LocalDateTime.now()),
            Warning("Warning D", WarningKilde.Spleis, LocalDateTime.now())
        )
        warningDao.leggTilWarnings(VEDTAKSPERIODE, testwarnings2)
        assertWarnings(testwarnings1 + testwarnings2, warningDao.finnWarnings(VEDTAKSPERIODE))
    }

    @Test
    fun `sletter gamle spleis-warnings og legger til nye`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val spesialistWarning = Warning("Warning B", WarningKilde.Spesialist, LocalDateTime.now())
        val testwarnings1 = listOf(Warning("Warning A", WarningKilde.Spleis, LocalDateTime.now()), spesialistWarning)
        warningDao.leggTilWarnings(VEDTAKSPERIODE, testwarnings1)
        val testwarnings2 = listOf(
            Warning("Warning C", WarningKilde.Spleis, LocalDateTime.now()),
            Warning("Warning D", WarningKilde.Spleis, LocalDateTime.now())
        )
        warningDao.oppdaterSpleisWarnings(VEDTAKSPERIODE, testwarnings2)
        assertWarnings((listOf(spesialistWarning) + testwarnings2), warningDao.finnWarnings(VEDTAKSPERIODE))
    }

    private fun assertWarnings(expected: List<Warning>, result: List<Warning>) {
        assertEquals(expected.size, result.size)
        assertEquals(expected, result)
    }
}

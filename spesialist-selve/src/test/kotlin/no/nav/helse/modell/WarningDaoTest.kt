package no.nav.helse.modell

import DatabaseIntegrationTest
import java.time.LocalDateTime
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.Test

internal class WarningDaoTest : DatabaseIntegrationTest() {
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
    }

    @Test
    fun `finn aktive warnings med meldingen Warning A`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val testWarningVurderMedlemskap = "Vurder lovvalg og medlemskap"

        val testwarning = listOf(Warning(testWarningVurderMedlemskap, WarningKilde.Spleis, LocalDateTime.now()))
        warningDao.leggTilWarnings(VEDTAKSPERIODE, testwarning)
    }

    @Test
    fun `finner ikke aktive warnings med meldingen Warning A`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val testWarningMeldingB = "Warning B"

        val testwarning = listOf(Warning(testWarningMeldingB, WarningKilde.Spleis, LocalDateTime.now()))
        warningDao.leggTilWarnings(VEDTAKSPERIODE, testwarning)
    }
}

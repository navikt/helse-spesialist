package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class UtbetalingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `hent utbetalinger for en person`() {
        nyPerson()
        val fagsystemId = fagsystemId()

        val arbeidsgiverOppdragId = lagOppdrag(fagsystemId)

        lagLinje(arbeidsgiverOppdragId, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val utbetalingId = lagUtbetalingId(arbeidsgiverOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingId, GODKJENT, LocalDateTime.now().minusDays(3), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingId, SENDT, LocalDateTime.now().minusDays(2), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingId, OVERFØRT, LocalDateTime.now().minusDays(1), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingId, UTBETALT, LocalDateTime.now(), "{}")

        val utbetalinger = utbetalingDao.findUtbetalinger(FNR)
        assertEquals(1, utbetalinger.size)
        val utbetaling = utbetalinger.first()

        assertEquals(UTBETALT, utbetaling.status)
        assertEquals("UTBETALING", utbetaling.type)
        assertEquals(fagsystemId, utbetaling.arbeidsgiverOppdrag.fagsystemId)
        assertEquals(ORGNUMMER, utbetaling.arbeidsgiverOppdrag.organisasjonsnummer)
        assertEquals(1, utbetaling.arbeidsgiverOppdrag.linjer.size)
    }

    @Test
    fun `henter riktige linjer for person`() {
        nyPerson()
        val oppdrag1 = lagOppdrag()
        lagLinje(oppdrag1, 1.januar(), 31.januar())
        lagLinje(oppdrag1, 1.februar(), 28.februar())

        val arbeidsgiverOppdragId = lagOppdrag()
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 31.juli())
        val utbetalingIdId = lagUtbetalingId(arbeidsgiverOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, UTBETALT, LocalDateTime.now(), "{}")

        val utbetaling = utbetalingDao.findUtbetalinger(FNR).first()

        assertEquals(listOf(UtbetalingDao.UtbetalingDto.OppdragDto.UtbetalingLinje(
            fom = 1.juli(),
            tom = 31.juli(),
            totalbeløp = null
        )), utbetaling.arbeidsgiverOppdrag.linjer)
    }
}

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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UtbetalingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `hent utbetalinger for en person`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)

        lagLinje(arbeidsgiverOppdragId, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val utbetalingId = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingId, GODKJENT, LocalDateTime.now().minusDays(3), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingId, SENDT, LocalDateTime.now().minusDays(2), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingId, OVERFØRT, LocalDateTime.now().minusDays(1), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingId, UTBETALT, LocalDateTime.now(), "{}")

        val utbetalinger = utbetalingDao.findUtbetalinger(FNR)
        assertEquals(1, utbetalinger.size)
        utbetalinger.first().let {
            assertEquals(UTBETALT, it.status)
            assertEquals("UTBETALING", it.type)
            assertEquals(arbeidsgiverFagsystemId, it.arbeidsgiveroppdrag!!.fagsystemId)
            assertEquals(personFagsystemId, it.personoppdrag!!.fagsystemId)
            assertEquals(ORGNUMMER, it.arbeidsgiveroppdrag.mottaker)
            assertEquals(FNR, it.personoppdrag.mottaker)
            assertEquals(1, it.arbeidsgiveroppdrag.linjer.size)
        }
    }

    @Test
    fun `henter riktige linjer for person`() {
        nyPerson()
        val oppdrag1 = lagArbeidsgiveroppdrag()
        lagLinje(oppdrag1, 1.januar(), 31.januar())
        lagLinje(oppdrag1, 1.februar(), 28.februar())

        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag()
        val personOppdragId = lagPersonoppdrag()
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 31.juli())
        val utbetalingIdId = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, UTBETALT, LocalDateTime.now(), "{}")

        utbetalingDao.findUtbetalinger(FNR).find { it.arbeidsgiveroppdrag!!.linjer.size == 1 }.let {
            assertNotNull(it)
            assertEquals(listOf(UtbetalingDao.UtbetalingDto.OppdragDto.UtbetalingLinje(
                fom = 1.juli(),
                tom = 31.juli(),
                totalbeløp = null
            )), it.arbeidsgiveroppdrag!!.linjer)
        }
    }

    @Test
    fun `henter ut personoppdrag`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 10.juli(), 12000)
        lagLinje(personOppdragId, 11.juli(), 31.juli(), 10000)
        val utbetaling = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetaling, UTBETALT, LocalDateTime.now(), "{}")

        val utbetalinger = utbetalingDao.findUtbetalinger(FNR)

        assertEquals(1, utbetalinger.size)
        assertTrue { utbetalinger.find { it.arbeidsgiveroppdrag!!.fagsystemId == arbeidsgiverFagsystemId } != null }
        assertTrue { utbetalinger.find { it.personoppdrag!!.fagsystemId == personFagsystemId } != null }
        assertTrue { utbetalinger.all { it.arbeidsgiveroppdrag!!.linjer.size == 1 } }
        assertTrue { utbetalinger.all { it.personoppdrag!!.linjer.size == 1 } }
    }
}

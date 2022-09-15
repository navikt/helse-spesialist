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
import java.util.UUID
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow

class UtbetalingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `Ukjent utbetaling har aldri endring`() {
        assertFalse(utbetalingDao.erUtbetaltFør(UUID.randomUUID()))
    }

    @Test
    fun `Annen utbetaling påvirker ikke sjekken`() {
        nyPerson()

        // Urelatert utbetaling
        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(fagsystemId())
        val personOppdragId1 = lagPersonoppdrag(fagsystemId())
        val utbetaling_idId1 = lagUtbetalingId(arbeidsgiveroppdragId1, personOppdragId1, UUID.randomUUID())
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId1, UTBETALT, LocalDateTime.now(), "{}")

        // Gjeldende utbetaling
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiveroppdragId2 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId2 = lagPersonoppdrag(personFagsystemId)
        val utbetalingId2 = UUID.randomUUID()
        val utbetaling_idId2 = lagUtbetalingId(arbeidsgiveroppdragId2, personOppdragId2, utbetalingId2)
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId2, IKKE_UTBETALT, LocalDateTime.now(), "{}")

        assertFalse(utbetalingDao.erUtbetaltFør(utbetalingId2))
    }

    @Test
    fun `Ingen tidligere utbetaling`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        // Første periode: til arbeidsgiver, ikke noe til person
        val arbeidsgiveroppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        val utbetalingId = UUID.randomUUID()
        val utbetaling_idId = lagUtbetalingId(arbeidsgiveroppdragId, personOppdragId, utbetalingId)
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId, IKKE_UTBETALT, LocalDateTime.now(), "{}")

        assertFalse(utbetalingDao.erUtbetaltFør(utbetalingId))
    }

    @Test
    fun `Bytte fra AG til person er endring`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        // Første periode: til arbeidsgiver, ikke noe til person
        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId)
        val utbetaling_idId = lagUtbetalingId(arbeidsgiveroppdragId1, personOppdragId1, UUID.randomUUID())
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId, UTBETALT, LocalDateTime.now(), "{}")

        // Neste periode, mottaker 100 % byttet
        val arbeidsgiverOppdragId2 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId, endringskode = "UEND")
        val personOppdragId2 = lagPersonoppdrag(personFagsystemId)
        val utbetalingId = UUID.randomUUID()
        lagUtbetalingId(arbeidsgiverOppdragId2, personOppdragId2, utbetalingId)

        assertTrue(utbetalingDao.erUtbetaltFør(utbetalingId))
    }

    @Test
    fun `Overstyring, ny versjon av utbetalingen er ikke endring`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        // Første utkast: til arbeidsgiver, ikke noe til person
        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId)
        val utbetaling_idId = lagUtbetalingId(arbeidsgiveroppdragId1, personOppdragId1, UUID.randomUUID())
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId, FORKASTET, LocalDateTime.now(), "{}")

        // Neste utkast
        val arbeidsgiverOppdragId2 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId2 = lagPersonoppdrag(personFagsystemId)
        val utbetalingId = UUID.randomUUID()
        lagUtbetalingId(arbeidsgiverOppdragId2, personOppdragId2, utbetalingId)

        assertFalse(utbetalingDao.erUtbetaltFør(utbetalingId))
    }

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
            assertEquals(ORGNUMMER, it.arbeidsgiveroppdrag!!.mottaker)
            assertEquals(FNR, it.personoppdrag!!.mottaker)
            assertEquals(1, it.arbeidsgiveroppdrag!!.linjer.size)
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
            if (it != null) {
                assertEquals(listOf(UtbetalingDao.UtbetalingDto.OppdragDto.UtbetalingLinje(
                    fom = 1.juli(),
                    tom = 31.juli(),
                    totalbeløp = null
                )), it.arbeidsgiveroppdrag!!.linjer)
            }
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

    @Test
    fun `alle enumer finnes også i db`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 10.juli(), 12000)
        lagLinje(personOppdragId, 11.juli(), 31.juli(), 10000)
        val utbetaling = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId)

        assertDoesNotThrow {
            Utbetalingsstatus.values().forEach {
                utbetalingDao.nyUtbetalingStatus(utbetaling, it, LocalDateTime.now(), "{}")
            }
        }
    }
}

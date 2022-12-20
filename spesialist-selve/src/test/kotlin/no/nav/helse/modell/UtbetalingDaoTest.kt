package no.nav.helse.modell

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.juli
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingtype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
    fun `lagrer personbeløp og arbeidsgiverbeløp på utbetaling`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        // Første utkast: til arbeidsgiver, ikke noe til person
        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId)
        val utbetalingId = UUID.randomUUID()
        utbetalingDao.opprettUtbetalingId(utbetalingId, FNR, ORGNUMMER, Utbetalingtype.UTBETALING, LocalDateTime.now(), arbeidsgiveroppdragId1, personOppdragId1, 2000, 2000)
        assertArbeidsgiverbeløp(2000, utbetalingId)
        assertPersonbeløp(2000, utbetalingId)
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

    private fun assertArbeidsgiverbeløp(beløp: Int, utbetalingId: UUID) {
        @Language("PostgreSQL")
        val query = "SELECT arbeidsgiverbeløp FROM utbetaling_id WHERE utbetaling_id = ?"
        val arbeidsgiverbeløp = sessionOf(dataSource).use {
            it.run(queryOf(query, utbetalingId).map { it.intOrNull("arbeidsgiverbeløp") }.asSingle)
        }
        assertEquals(beløp, arbeidsgiverbeløp)
    }

    private fun assertPersonbeløp(beløp: Int, utbetalingId: UUID) {
        @Language("PostgreSQL")
        val query = "SELECT personbeløp FROM utbetaling_id WHERE utbetaling_id = ?"
        val personbeløp = sessionOf(dataSource).use {
            it.run(queryOf(query, utbetalingId).map { it.intOrNull("personbeløp") }.asSingle)
        }
        assertEquals(beløp, personbeløp)
    }
}

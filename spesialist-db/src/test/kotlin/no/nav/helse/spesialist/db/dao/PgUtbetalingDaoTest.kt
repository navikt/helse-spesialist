package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

class PgUtbetalingDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `finner utbetaling`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId)
        val utbetalingId = UUID.randomUUID()
        utbetalingDao.opprettUtbetalingId(
            utbetalingId,
            FNR,
            ORGNUMMER,
            Utbetalingtype.UTBETALING,
            LocalDateTime.now(),
            arbeidsgiveroppdragId1,
            personOppdragId1,
            2000,
            2000,
        )

        val utbetaling = utbetalingDao.hentUtbetaling(utbetalingId)
        assertEquals(Utbetaling(utbetalingId, 2000, 2000, Utbetalingtype.UTBETALING), utbetaling)
    }

    @Test
    fun `lagrer personbeløp og arbeidsgiverbeløp på utbetaling`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId)
        val utbetalingId = UUID.randomUUID()
        utbetalingDao.opprettUtbetalingId(
            utbetalingId,
            FNR,
            ORGNUMMER,
            Utbetalingtype.UTBETALING,
            LocalDateTime.now(),
            arbeidsgiveroppdragId1,
            personOppdragId1,
            2000,
            2000,
        )
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
        val utbetaling = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId)

        assertDoesNotThrow {
            Utbetalingsstatus.entries.forEach {
                utbetalingDao.nyUtbetalingStatus(utbetaling, it, LocalDateTime.now(), "{}")
            }
        }
    }

    private fun assertArbeidsgiverbeløp(
        beløp: Int,
        utbetalingId: UUID,
    ) {
        val arbeidsgiverbeløp = dbQuery.single(
            "SELECT arbeidsgiverbeløp FROM utbetaling_id WHERE utbetaling_id = :utbetalingId",
            "utbetalingId" to utbetalingId,
        ) { it.intOrNull("arbeidsgiverbeløp") }
        assertEquals(beløp, arbeidsgiverbeløp)
    }

    private fun assertPersonbeløp(
        beløp: Int,
        utbetalingId: UUID,
    ) {
        val personbeløp = dbQuery.single(
            "SELECT personbeløp FROM utbetaling_id WHERE utbetaling_id = :utbetalingId",
            "utbetalingId" to utbetalingId,
        ) { it.intOrNull("personbeløp") }
        assertEquals(beløp, personbeløp)
    }
}

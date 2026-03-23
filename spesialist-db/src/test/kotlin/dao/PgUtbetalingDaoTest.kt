package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

class PgUtbetalingDaoTest : AbstractDBIntegrationTest() {
    private val arbeidsgiver = opprettArbeidsgiver()
    private val person =
        opprettPerson().also {
            opprettVedtaksperiode(it, arbeidsgiver).also { vedtaksperiode ->
                opprettBehandling(vedtaksperiode).also { behandling ->
                    opprettOppgave(vedtaksperiode, behandling)
                }
            }
        }

    @Test
    fun `finner utbetaling`() {
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId, person.id.value)
        val utbetalingId = UUID.randomUUID()
        utbetalingDao.opprettUtbetalingId(
            utbetalingId,
            person.id.value,
            arbeidsgiver.organisasjonsnummer,
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
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        val arbeidsgiveroppdragId1 = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId1 = lagPersonoppdrag(personFagsystemId, person.id.value)
        val utbetalingId = UUID.randomUUID()
        utbetalingDao.opprettUtbetalingId(
            utbetalingId,
            person.id.value,
            arbeidsgiver.organisasjonsnummer,
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
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId, person.id.value)
        val utbetaling = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, fødselsnummer = person.id.value, organisasjonsnummer = arbeidsgiver.organisasjonsnummer)

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
        val arbeidsgiverbeløp =
            dbQuery.single(
                "SELECT arbeidsgiverbeløp FROM utbetaling_id WHERE utbetaling_id = :utbetalingId",
                "utbetalingId" to utbetalingId,
            ) { it.intOrNull("arbeidsgiverbeløp") }
        assertEquals(beløp, arbeidsgiverbeløp)
    }

    private fun assertPersonbeløp(
        beløp: Int,
        utbetalingId: UUID,
    ) {
        val personbeløp =
            dbQuery.single(
                "SELECT personbeløp FROM utbetaling_id WHERE utbetaling_id = :utbetalingId",
                "utbetalingId" to utbetalingId,
            ) { it.intOrNull("personbeløp") }
        assertEquals(beløp, personbeløp)
    }

    private val Arbeidsgiver.organisasjonsnummer get() =
        when (val id = this.id) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
        }
}

package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*
import kotlin.test.assertEquals

internal class UtbetalingE2ETest : AbstractE2ETest() {

    private companion object {
        private const val ORGNR = "987654321"
        private const val arbeidsgiverFagsystemId = "ASDJ12IA312KLS"

        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    @Test
    fun `utbetaling endret`() {
        vedtaksperiode(ORGNR, VEDTAKSPERIODE_ID, true, SNAPSHOTV1_MED_WARNINGS, UTBETALING_ID)
        sendUtbetalingEndret("UTBETALING", GODKJENT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("ETTERUTBETALING", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("ANNULLERING", ANNULLERT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        assertEquals(3, utbetalinger().size)
    }

    @Test
    fun `utbetaling endret uten at vi kjenner arbeidsgiver`() {
        val ET_ORGNR = "1"
        val ET_ANNET_ORGNR = "2"
        vedtaksperiode(ET_ORGNR, VEDTAKSPERIODE_ID, true, SNAPSHOTV1_MED_WARNINGS, UTBETALING_ID)
        assertDoesNotThrow {
            sendUtbetalingEndret("UTBETALING", GODKJENT, ET_ANNET_ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        }
        assertEquals(0, utbetalinger().size)
        assertEquals(1, feilendeMeldinger().size)
    }

    @Test
    fun `utbetaling forkastet`() {
        vedtaksperiode(ORGNR, VEDTAKSPERIODE_ID, true, SNAPSHOTV1_MED_WARNINGS, UTBETALING_ID)
        sendUtbetalingEndret(
            "UTBETALING",
            FORKASTET,
            ORGNR,
            arbeidsgiverFagsystemId,
            forrigeStatus = IKKE_GODKJENT,
            utbetalingId = UTBETALING_ID
        )
        assertEquals(0, utbetalinger().size)
        sendUtbetalingEndret(
            "UTBETALING",
            FORKASTET,
            ORGNR,
            arbeidsgiverFagsystemId,
            forrigeStatus = GODKJENT,
            utbetalingId = UTBETALING_ID
        )
        assertEquals(1, utbetalinger().size)
    }

    @Test
    fun `legger på totalbeløp på utbetaling`() {
        vedtaksperiode(ORGNR, VEDTAKSPERIODE_ID, true, SNAPSHOTV1_MED_WARNINGS, UTBETALING_ID)
        sendUtbetalingEndret("ETTERUTBETALING", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)

        assertEquals(4000, utbetalingDao.findUtbetalinger(FØDSELSNUMMER).single().totalbeløp)
    }

    @Test
    fun `feriepengeutbetalinger har riktig type på utbetaling`() {
        vedtaksperiode(utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("FERIEPENGER", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)

        assertEquals("FERIEPENGER", utbetalingDao.findUtbetalinger(FØDSELSNUMMER).single().type)
    }

    @Test
    fun `ved endringer i utbetalinger skal kun nyeste vises`() {
        val nyUtbetalingId = UUID.randomUUID()
        vedtaksperiode(utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("FERIEPENGER", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("FERIEPENGER", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = nyUtbetalingId)

        assertEquals(1, utbetalingDao.findUtbetalinger(FØDSELSNUMMER).size)
        assertEquals(nyUtbetalingId, utbetalingDao.findUtbetalinger(FØDSELSNUMMER).single().utbetalingId)
    }

    private fun utbetalinger(): List<Long> {
        @Language("PostgreSQL")
        val statement = """
            SELECT u.*
            FROM utbetaling u
            INNER JOIN utbetaling_id ui ON (ui.id = u.utbetaling_id_ref)
            INNER JOIN person p ON (p.id = ui.person_ref)
            INNER JOIN arbeidsgiver a ON (a.id = ui.arbeidsgiver_ref)
            INNER JOIN oppdrag o1 ON (o1.id = ui.arbeidsgiver_fagsystem_id_ref)
            INNER JOIN oppdrag o2 ON (o2.id = ui.person_fagsystem_id_ref)
            WHERE p.fodselsnummer = :fodselsnummer AND a.orgnummer = :orgnummer
            """
        return sessionOf(dataSource).use  {
            it.run(queryOf(statement, mapOf(
                "fodselsnummer" to FØDSELSNUMMER.toLong(),
                "orgnummer" to ORGNR.toLong()
            )).map { row -> row.long("utbetaling_id_ref") }.asList)
        }
    }

    private fun feilendeMeldinger(): List<UUID> {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM feilende_meldinger"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement).map { UUID.fromString(it.string("id")) }.asList)
        }
    }
}

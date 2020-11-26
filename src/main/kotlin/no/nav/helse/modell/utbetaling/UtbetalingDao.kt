package no.nav.helse.modell.utbetaling

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class UtbetalingDao(private val dataSource: DataSource) {
    fun lagre(
        utbetalingId: UUID,
        fødselsnummer: String,
        orgnummer: String,
        type: String,
        status: String,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
        json: String
    ) {
        opprettUtbetalingId(utbetalingId)
        opprettOppdrag(arbeidsgiverFagsystemId)
        opprettOppdrag(personFagsystemId)

        @Language("PostgreSQL")
        val statement = """
                INSERT INTO utbetaling (
                    utbetaling_id_ref, person_ref, arbeidsgiver_ref, type,
                    status, opprettet, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref, data
                ) VALUES (
                    (SELECT id FROM utbetaling_id WHERE utbetaling_id = :utbetalingId),
                    (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                    (SELECT id FROM arbeidsgiver WHERE orgnummer = :orgnummer),
                    CAST(:type as utbetaling_type),
                    CAST(:status as utbetaling_status),
                    :opprettet,
                    (SELECT id FROM oppdrag WHERE fagsystem_id = :arbeidsgiverFagsystemId),
                    (SELECT id FROM oppdrag WHERE fagsystem_id = :personFagsystemId),
                    CAST(:json as json)
                )
        """
        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "utbetalingId" to utbetalingId,
                "fodselsnummer" to fødselsnummer.toLong(),
                "orgnummer" to orgnummer.toLong(),
                "type" to type,
                "status" to status,
                "opprettet" to opprettet,
                "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
                "personFagsystemId" to personFagsystemId,
                "json" to json
            )).asExecute)
        }
    }

    private fun opprettOppdrag(fagsystemId: String) {
        @Language("PostgreSQL")
        val statement = """INSERT INTO oppdrag (fagsystem_id) VALUES (:fagsystemId) ON CONFLICT DO NOTHING"""
        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "fagsystemId" to fagsystemId
            )).asExecute)
        }
    }

    private fun opprettUtbetalingId(utbetalingId: UUID) {
        @Language("PostgreSQL")
        val statement = """INSERT INTO utbetaling_id (utbetaling_id) VALUES (:utbetalingId) ON CONFLICT DO NOTHING"""
        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "utbetalingId" to utbetalingId
            )).asExecute)
        }
    }

}

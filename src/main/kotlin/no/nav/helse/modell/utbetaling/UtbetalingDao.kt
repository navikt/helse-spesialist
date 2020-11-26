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
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO utbetaling (utbetaling_id, person_ref, arbeidsgiver_ref, type,
                    status, opprettet, arbeidsgiver_fagsystem_id, person_fagsystem_id, data)
                VALUES (:utbetalingId, (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                    (SELECT id FROM arbeidsgiver WHERE orgnummer = :orgnummer), CAST(:type as utbetaling_type),
                    CAST(:status as utbetaling_status), :opprettet, :arbeidsgiverFagsystemId, :personFagsystemId, CAST(:json as json))
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

}

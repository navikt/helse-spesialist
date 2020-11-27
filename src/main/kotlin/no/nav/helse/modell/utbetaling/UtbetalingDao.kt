package no.nav.helse.modell.utbetaling

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class UtbetalingDao(private val dataSource: DataSource) {
    internal fun finnUtbetalingIdRef(utbetalingId: UUID): Long? {
        val statement = """
            SELECT id FROM utbetaling_id WHERE utbetaling_id = ? LIMIT 1
        """
        return using(sessionOf(dataSource)) {
            it.run(queryOf(statement, utbetalingId).map {
                it.long("id")
            }.asSingle)
        }
    }

    fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: String,
        opprettet: LocalDateTime,
        json: String
    ) {
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO utbetaling ( utbetaling_id_ref, status, opprettet, data )
                VALUES (:utbetalingIdRef, CAST(:status as utbetaling_status), :opprettet, CAST(:json as json))
        """
        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "utbetalingIdRef" to utbetalingIdRef,
                "status" to status,
                "opprettet" to opprettet,
                "json" to json
            )).asExecute)
        }
    }

    internal fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        orgnummer: String,
        type: String,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long
    ): Long {
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO utbetaling_id (
                    utbetaling_id, person_ref, arbeidsgiver_ref, type, opprettet, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref
                ) VALUES (
                    :utbetalingId,
                    (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                    (SELECT id FROM arbeidsgiver WHERE orgnummer = :orgnummer),
                    CAST(:type as utbetaling_type),
                    :opprettet,
                    :arbeidsgiverFagsystemIdRef,
                    :personFagsystemIdRef
                )
                ON CONFLICT (utbetaling_id) DO NOTHING RETURNING id
        """
        return using(sessionOf(dataSource, returnGeneratedKey = true)) {
            requireNotNull(it.run(queryOf(statement, mapOf(
                "utbetalingId" to utbetalingId,
                "fodselsnummer" to fødselsnummer.toLong(),
                "orgnummer" to orgnummer.toLong(),
                "type" to type,
                "opprettet" to opprettet,
                "arbeidsgiverFagsystemIdRef" to arbeidsgiverFagsystemIdRef,
                "personFagsystemIdRef" to personFagsystemIdRef
            )).asUpdateAndReturnGeneratedKey)) { "Kunne ikke opprette utbetaling" }
        }
    }

    internal fun nyttOppdrag(
        fagsystemId: String,
        mottaker: String,
        fagområde: String,
        endringskode: String,
        sisteArbeidsgiverdag: LocalDate?
    ): Long? {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO oppdrag (fagsystem_id, mottaker, fagområde, endringskode, sisteArbeidsgiverdag)
            VALUES (:fagsystemId, :mottaker, CAST(:fagomrade as oppdrag_fagområde), CAST(:endringskode as oppdrag_endringskode), :sisteArbeidsgiverdag)
        """
        return using(sessionOf(dataSource, returnGeneratedKey = true)) {
            it.run(queryOf(statement, mapOf(
                "fagsystemId" to fagsystemId,
                "mottaker" to mottaker,
                "fagomrade" to fagområde,
                "endringskode" to endringskode,
                "sisteArbeidsgiverdag" to sisteArbeidsgiverdag
            )).asUpdateAndReturnGeneratedKey)
        }
    }

    internal fun nyLinje(
        oppdragId: Long,
        endringskode: String,
        klassekode: String,
        statuskode: String?,
        datoStatusFom: LocalDate?,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        lønn: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO utbetalingslinje(oppdrag_id, delytelseId, refdelytelseid, reffagsystemid, endringskode, klassekode, statuskode, datostatusfom, fom, tom, dagsats, lønn, grad)
            VALUES (:oppdragIdRef, :delytelseId, :refDelytelseId, :refFagsystemId, CAST(:endringskode as oppdrag_endringskode), CAST(:klassekode as oppdrag_klassekode),
            CAST(:statuskode as oppdrag_statuskode), :datoStatusFom, :fom, :tom, :dagsats, :lonn, :grad)
        """
        return using(sessionOf(dataSource)) {
            it.run(queryOf(statement, mapOf(
                "oppdragIdRef" to oppdragId,
                "delytelseId" to delytelseId,
                "refDelytelseId" to refDelytelseId,
                "refFagsystemId" to refFagsystemId,
                "endringskode" to endringskode,
                "klassekode" to klassekode,
                "statuskode" to statuskode,
                "datoStatusFom" to datoStatusFom,
                "fom" to fom,
                "tom" to tom,
                "dagsats" to dagsats,
                "lonn" to lønn,
                "grad" to grad
            )).asExecute)
        }
    }

}

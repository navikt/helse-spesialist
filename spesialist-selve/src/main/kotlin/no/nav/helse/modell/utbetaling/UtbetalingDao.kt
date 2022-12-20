package no.nav.helse.modell.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class UtbetalingDao(private val dataSource: DataSource) {

    fun erUtbetaltFør(utbetalingId: UUID): Boolean {
        @Language("PostgreSQL")
        val statement = """
            select 1 from utbetaling_id
             join oppdrag oppdrag_1 on oppdrag_1.id = utbetaling_id.arbeidsgiver_fagsystem_id_ref or oppdrag_1.id = utbetaling_id.person_fagsystem_id_ref
             join oppdrag oppdrag_2 on oppdrag_2.fagsystem_id = oppdrag_1.fagsystem_id
             join utbetaling_id utbetaling_id_2 on utbetaling_id_2.arbeidsgiver_fagsystem_id_ref = oppdrag_2.id or utbetaling_id_2.person_fagsystem_id_ref = oppdrag_2.id  
             join utbetaling on utbetaling_id_2.id = utbetaling.utbetaling_id_ref and status = 'UTBETALT' 
             where utbetaling_id.utbetaling_id = :utbetalingId
        """.trimIndent()
        return sessionOf(dataSource).use {
            it.run(queryOf(statement, mapOf("utbetalingId" to utbetalingId)).map { it }.asList).isNotEmpty()
        }
    }
    internal fun finnUtbetalingIdRef(utbetalingId: UUID): Long? {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM utbetaling_id WHERE utbetaling_id = ? LIMIT 1;"
        return sessionOf(dataSource).use {
            it.run(queryOf(statement, utbetalingId).map { row ->
                row.long("id")
            }.asSingle)
        }
    }

    fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO utbetaling ( utbetaling_id_ref, status, opprettet, data )
            VALUES (:utbetalingIdRef, CAST(:status as utbetaling_status), :opprettet, CAST(:json as json)) ON CONFLICT (status, opprettet, utbetaling_id_ref) DO NOTHING;
        """
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    statement, mapOf(
                        "utbetalingIdRef" to utbetalingIdRef,
                        "status" to status.toString(),
                        "opprettet" to opprettet,
                        "json" to json
                    )
                ).asExecute
            )
        }
    }

    internal fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        orgnummer: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long,
        arbeidsgiverbeløp: Int,
        personbeløp: Int
    ): Long {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO utbetaling_id (
                utbetaling_id, person_ref, arbeidsgiver_ref, type, opprettet, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref, arbeidsgiverbeløp, personbeløp
            ) VALUES (
                :utbetalingId,
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                (SELECT id FROM arbeidsgiver WHERE orgnummer = :orgnummer),
                CAST(:type as utbetaling_type),
                :opprettet,
                :arbeidsgiverFagsystemIdRef,
                :personFagsystemIdRef,
                :arbeidsgiverbelop,
                :personbelop
            )
            ON CONFLICT (utbetaling_id) DO NOTHING RETURNING id
        """
        return sessionOf(dataSource, returnGeneratedKey = true).use {
            requireNotNull(
                it.run(
                    queryOf(
                        statement, mapOf(
                            "utbetalingId" to utbetalingId,
                            "fodselsnummer" to fødselsnummer.toLong(),
                            "orgnummer" to orgnummer.toLong(),
                            "type" to type.toString(),
                            "opprettet" to opprettet,
                            "arbeidsgiverFagsystemIdRef" to arbeidsgiverFagsystemIdRef,
                            "personFagsystemIdRef" to personFagsystemIdRef,
                            "arbeidsgiverbelop" to arbeidsgiverbeløp,
                            "personbelop" to personbeløp,
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
            ) { "Kunne ikke opprette utbetaling" }
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
            ON CONFLICT DO NOTHING
        """
        return sessionOf(dataSource, returnGeneratedKey = true).use {
            it.run(
                queryOf(
                    statement, mapOf(
                        "fagsystemId" to fagsystemId,
                        "mottaker" to mottaker,
                        "fagomrade" to fagområde,
                        "endringskode" to endringskode,
                        "sisteArbeidsgiverdag" to sisteArbeidsgiverdag
                    )
                ).asUpdateAndReturnGeneratedKey
            )
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
        totalbeløp: Int?,
        lønn: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO utbetalingslinje(oppdrag_id, delytelseId, refdelytelseid, reffagsystemid, endringskode, klassekode, statuskode, datostatusfom, fom, tom, dagsats, totalbeløp, lønn, grad)
            VALUES (:oppdragIdRef, :delytelseId, :refDelytelseId, :refFagsystemId, CAST(:endringskode as oppdrag_endringskode), CAST(:klassekode as oppdrag_klassekode),
            CAST(:statuskode as oppdrag_statuskode), :datoStatusFom, :fom, :tom, :dagsats, :totalbelop, :lonn, :grad)
        """
        return sessionOf(dataSource).use {
            it.run(
                queryOf(
                    statement, mapOf(
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
                        "totalbelop" to totalbeløp,
                        "lonn" to lønn,
                        "grad" to grad
                    )
                ).asExecute
            )
        }
    }

    internal fun nyAnnullering(annullertTidspunkt: LocalDateTime, saksbehandlerRef: UUID): Long {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO annullert_av_saksbehandler(annullert_tidspunkt, saksbehandler_ref)
            VALUES (:annullertTidspunkt, :saksbehandlerRef)
        """
        return sessionOf(dataSource, returnGeneratedKey = true).use {
            requireNotNull(
                it.run(
                    queryOf(
                        statement, mapOf(
                            "annullertTidspunkt" to annullertTidspunkt,
                            "saksbehandlerRef" to saksbehandlerRef
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
            ) { "Kunne ikke opprette annullering" }
        }
    }

    fun leggTilAnnullertAvSaksbehandler(utbetalingId: UUID, annullertAvSaksbehandlerRef: Long) {
        val utbetalingIdRef = finnUtbetalingIdRef(utbetalingId)

        @Language("PostgreSQL")
        val query = """
            UPDATE utbetaling
                SET annullert_av_saksbehandler_ref = :annullertAvSaksbehandlerRef
            WHERE utbetaling_id_ref = :utbetalingIdRef
        """

        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query, mapOf(
                        "annullertAvSaksbehandlerRef" to annullertAvSaksbehandlerRef,
                        "utbetalingIdRef" to utbetalingIdRef
                    )
                ).asExecute
            )
        }
    }

    internal fun opprettKobling(vedtaksperiodeId: UUID, utbetalingId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_utbetaling_id(vedtaksperiode_id, utbetaling_id) VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """
        session.run(queryOf(statement, vedtaksperiodeId, utbetalingId).asUpdate)
    }

    internal fun fjernKobling(vedtaksperiodeId: UUID, utbetalingId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "DELETE FROM vedtaksperiode_utbetaling_id WHERE utbetaling_id = ? AND vedtaksperiode_id = ?"
            session.run(queryOf(statement, utbetalingId, vedtaksperiodeId).asUpdate)
        }

    data class TidligereUtbetalingerForVedtaksperiodeDto(
        val utbetalingId: UUID,
        val id: Int,
        val utbetalingsstatus: Utbetalingsstatus
    )

    internal fun utbetalingerForVedtaksperiode(vedtaksperiodeId: UUID): List<TidligereUtbetalingerForVedtaksperiodeDto> {
        @Language("PostgreSQL")
        val statement = """
            SELECT vui.utbetaling_id, u.id, u.status
            FROM vedtaksperiode_utbetaling_id vui
            JOIN utbetaling_id ui ON ui.utbetaling_id = vui.utbetaling_id
            JOIN utbetaling u ON u.utbetaling_id_ref = ui.id
            WHERE vui.vedtaksperiode_id = :vedtaksperiodeId
            ORDER BY u.id DESC
            LIMIT 2;
        """
        return sessionOf(dataSource).use {
            it.run(
                queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId))
                    .map { row -> TidligereUtbetalingerForVedtaksperiodeDto(
                        id = row.int("id"),
                        utbetalingId = row.uuid("utbetaling_id"),
                        utbetalingsstatus = Utbetalingsstatus.valueOf(row.string("status"))
                    )}.asList
            )
        }
    }
}

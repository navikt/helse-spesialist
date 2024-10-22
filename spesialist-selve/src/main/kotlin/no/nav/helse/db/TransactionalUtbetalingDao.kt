package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.UtbetalingDao.TidligereUtbetalingerForVedtaksperiodeDto
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class TransactionalUtbetalingDao(private val session: Session) : UtbetalingRepository {
    override fun sisteUtbetalingIdFor(fødselsnummer: String): UUID? {
        @Language("PostgreSQL")
        val query =
            """
            select ui.utbetaling_id from utbetaling_id ui 
            join person p on ui.person_ref = p.id 
            where p.fodselsnummer = :fnr
            order by ui.id desc
            limit 1;
            """.trimIndent()
        return session.run(
            queryOf(query, mapOf("fnr" to fødselsnummer.toLong())).map {
                it.uuid("utbetaling_id")
            }.asSingle,
        )
    }

    override fun finnUtbetalingIdRef(utbetalingId: UUID): Long? {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM utbetaling_id WHERE utbetaling_id = ? LIMIT 1;"
        return session.run(
            queryOf(statement, utbetalingId).map { row ->
                row.long("id")
            }.asSingle,
        )
    }

    override fun hentUtbetaling(utbetalingId: UUID): Utbetaling =
        checkNotNull(utbetalingFor(utbetalingId)) { "Finner ikke utbetaling, utbetalingId=$utbetalingId" }

    override fun utbetalingFor(utbetalingId: UUID): Utbetaling? {
        @Language("PostgreSQL")
        val query =
            "SELECT arbeidsgiverbeløp, personbeløp, type FROM utbetaling_id u WHERE u.utbetaling_id = :utbetaling_id"
        return session.run(
            queryOf(query, mapOf("utbetaling_id" to utbetalingId)).map {
                Utbetaling(
                    utbetalingId,
                    it.int("arbeidsgiverbeløp"),
                    it.int("personbeløp"),
                    enumValueOf(it.string("type")),
                )
            }.asSingle,
        )
    }

    override fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String,
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO utbetaling ( utbetaling_id_ref, status, opprettet, data )
            VALUES (:utbetalingIdRef, CAST(:status as utbetaling_status), :opprettet, CAST(:json as json)) ON CONFLICT (status, opprettet, utbetaling_id_ref) DO NOTHING;
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "utbetalingIdRef" to utbetalingIdRef,
                    "status" to status.toString(),
                    "opprettet" to opprettet,
                    "json" to json,
                ),
            ).asExecute,
        )
    }

    override fun nyttOppdrag(
        fagsystemId: String,
        mottaker: String,
    ): Long? {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO oppdrag (fagsystem_id, mottaker)
            VALUES (:fagsystemId, :mottaker)
            ON CONFLICT DO NOTHING
        """
        return session.run(
            queryOf(
                statement,
                mapOf(
                    "fagsystemId" to fagsystemId,
                    "mottaker" to mottaker,
                ),
            ).asUpdateAndReturnGeneratedKey,
        )
    }

    override fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        orgnummer: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long,
        arbeidsgiverbeløp: Int,
        personbeløp: Int,
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
        return requireNotNull(
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "utbetalingId" to utbetalingId,
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnummer" to orgnummer.toLong(),
                        "type" to type.toString(),
                        "opprettet" to opprettet,
                        "arbeidsgiverFagsystemIdRef" to arbeidsgiverFagsystemIdRef,
                        "personFagsystemIdRef" to personFagsystemIdRef,
                        "arbeidsgiverbelop" to arbeidsgiverbeløp,
                        "personbelop" to personbeløp,
                    ),
                ).asUpdateAndReturnGeneratedKey,
            ),
        ) { "Kunne ikke opprette utbetaling" }
    }

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_utbetaling_id(vedtaksperiode_id, utbetaling_id) VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """
        session.run(queryOf(statement, vedtaksperiodeId, utbetalingId).asUpdate)
    }

    override fun fjernKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement = "DELETE FROM vedtaksperiode_utbetaling_id WHERE utbetaling_id = ? AND vedtaksperiode_id = ?"
        session.run(queryOf(statement, utbetalingId, vedtaksperiodeId).asUpdate)
    }

    override fun utbetalingerForVedtaksperiode(vedtaksperiodeId: UUID): List<TidligereUtbetalingerForVedtaksperiodeDto> {
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
        return session.run(
            queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map { row ->
                TidligereUtbetalingerForVedtaksperiodeDto(
                    id = row.int("id"),
                    utbetalingId = row.uuid("utbetaling_id"),
                    utbetalingsstatus = Utbetalingsstatus.valueOf(row.string("status")),
                )
            }.asList,
        )
    }

    override fun erUtbetalingForkastet(utbetalingId: UUID): Boolean {
        @Language("PostgreSQL")
        val query =
            """
            SELECT 1
            FROM utbetaling u
            JOIN utbetaling_id ui ON u.utbetaling_id_ref = ui.id
            WHERE ui.utbetaling_id = :utbetaling_id
            AND status = 'FORKASTET'
            """.trimIndent()
        return session.run(
            queryOf(query, mapOf("utbetaling_id" to utbetalingId)).map { true }.asSingle,
        ) ?: false
    }
}

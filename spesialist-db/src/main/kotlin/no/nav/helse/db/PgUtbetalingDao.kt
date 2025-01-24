package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.UtbetalingDao.TidligereUtbetalingerForVedtaksperiodeDto
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import java.time.LocalDateTime
import java.util.UUID

class PgUtbetalingDao internal constructor(session: Session) : UtbetalingDao, QueryRunner by MedSession(session) {
    override fun finnUtbetalingIdRef(utbetalingId: UUID): Long? =
        asSQL(
            """
            SELECT id FROM utbetaling_id WHERE utbetaling_id = :utbetalingId LIMIT 1;
            """.trimIndent(),
            "utbetalingId" to utbetalingId,
        ).singleOrNull { it.long("id") }

    override fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String,
    ) {
        asSQL(
            """
                INSERT INTO utbetaling ( utbetaling_id_ref, status, opprettet, data )
            VALUES (:utbetalingIdRef, CAST(:status as utbetaling_status), :opprettet, CAST(:json as json)) ON CONFLICT (status, opprettet, utbetaling_id_ref) DO NOTHING;
            """.trimIndent(),
            "utbetalingIdRef" to utbetalingIdRef,
            "status" to status.toString(),
            "opprettet" to opprettet,
            "json" to json,
        ).update()
    }

    override fun erUtbetalingForkastet(utbetalingId: UUID): Boolean =
        asSQL(
            """
            SELECT 1
            FROM utbetaling u
            JOIN utbetaling_id ui ON u.utbetaling_id_ref = ui.id
            WHERE ui.utbetaling_id = :utbetaling_id
            AND status = 'FORKASTET'
            """.trimIndent(),
            "utbetaling_id" to utbetalingId,
        ).singleOrNull { true } ?: false

    override fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long,
        arbeidsgiverbeløp: Int,
        personbeløp: Int,
    ): Long =
        asSQL(
            """
            INSERT INTO utbetaling_id (
                utbetaling_id, person_ref, arbeidsgiver_ref, type, opprettet, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref, arbeidsgiverbeløp, personbeløp
            ) VALUES (
                :utbetalingId,
                (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer),
                (SELECT id FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer),
                CAST(:type as utbetaling_type),
                :opprettet,
                :arbeidsgiverFagsystemIdRef,
                :personFagsystemIdRef,
                :arbeidsgiverbelop,
                :personbelop
            )
            ON CONFLICT (utbetaling_id) DO NOTHING RETURNING id
        """
                .trimIndent(),
            "utbetalingId" to utbetalingId,
            "fodselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "type" to type.toString(),
            "opprettet" to opprettet,
            "arbeidsgiverFagsystemIdRef" to arbeidsgiverFagsystemIdRef,
            "personFagsystemIdRef" to personFagsystemIdRef,
            "arbeidsgiverbelop" to arbeidsgiverbeløp,
            "personbelop" to personbeløp,
        ).updateAndReturnGeneratedKey()

    override fun nyttOppdrag(
        fagsystemId: String,
        mottaker: String,
    ): Long? =
        asSQL(
            """
                INSERT INTO oppdrag (fagsystem_id, mottaker)
            VALUES (:fagsystemId, :mottaker)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            "fagsystemId" to fagsystemId,
            "mottaker" to mottaker,
        ).updateAndReturnGeneratedKeyOrNull()

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        asSQL(
            """
            INSERT INTO vedtaksperiode_utbetaling_id(vedtaksperiode_id, utbetaling_id) VALUES (:vedtaksperiode_id, :utbetaling_id)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
        ).update()
    }

    override fun hentUtbetaling(utbetalingId: UUID): Utbetaling =
        checkNotNull(utbetalingFor(utbetalingId)) { "Finner ikke utbetaling, utbetalingId=$utbetalingId" }

    override fun utbetalingFor(utbetalingId: UUID): Utbetaling? =
        asSQL(
            """
            SELECT arbeidsgiverbeløp, personbeløp, type FROM utbetaling_id u WHERE u.utbetaling_id = :utbetaling_id
            """.trimIndent(),
            "utbetaling_id" to utbetalingId,
        ).singleOrNull {
            Utbetaling(
                utbetalingId,
                it.int("arbeidsgiverbeløp"),
                it.int("personbeløp"),
                enumValueOf(it.string("type")),
            )
        }

    override fun utbetalingFor(oppgaveId: Long): Utbetaling? =
        asSQL(
            """SELECT utbetaling_id, arbeidsgiverbeløp, personbeløp, type FROM utbetaling_id u WHERE u.utbetaling_id = (SELECT utbetaling_id FROM oppgave o WHERE o.id = :oppgave_id)"""
                .trimIndent(),
            "oppgave_id" to oppgaveId,
        ).singleOrNull {
            Utbetaling(
                it.uuid("utbetaling_id"),
                it.int("arbeidsgiverbeløp"),
                it.int("personbeløp"),
                enumValueOf(it.string("type")),
            )
        }

    override fun utbetalingerForVedtaksperiode(vedtaksperiodeId: UUID): List<TidligereUtbetalingerForVedtaksperiodeDto> =
        asSQL(
            """
            SELECT vui.utbetaling_id, u.id, u.status
            FROM vedtaksperiode_utbetaling_id vui
            JOIN utbetaling_id ui ON ui.utbetaling_id = vui.utbetaling_id
            JOIN utbetaling u ON u.utbetaling_id_ref = ui.id
            WHERE vui.vedtaksperiode_id = :vedtaksperiodeId
            ORDER BY u.id DESC
            LIMIT 2;
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).list {
            TidligereUtbetalingerForVedtaksperiodeDto(
                id = it.int("id"),
                utbetalingId = it.uuid("utbetaling_id"),
                utbetalingsstatus = Utbetalingsstatus.valueOf(it.string("status")),
            )
        }
}

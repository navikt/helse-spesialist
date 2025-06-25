package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
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
        arbeidsgiverIdentifikator: String,
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
                utbetaling_id, person_ref, arbeidsgiver_identifikator, type, opprettet, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref, arbeidsgiverbeløp, personbeløp
            ) VALUES (
                :utbetalingId,
                (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer),
                :arbeidsgiver_identifikator,
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
            "arbeidsgiver_identifikator" to arbeidsgiverIdentifikator,
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

    private fun utbetalingFor(utbetalingId: UUID): Utbetaling? =
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
}

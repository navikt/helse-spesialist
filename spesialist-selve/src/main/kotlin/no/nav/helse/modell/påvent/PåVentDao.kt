package no.nav.helse.modell.påvent

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.PåVentRepository
import no.nav.helse.db.QueryRunner
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PåVentDao(private val queryRunner: QueryRunner) : PåVentRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    fun lagrePåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
    ) = asSQL(
        """
        SELECT v.vedtaksperiode_id
        FROM vedtak v
        INNER JOIN oppgave o on v.id = o.vedtak_ref
        WHERE o.id = :oppgaveId
        """.trimIndent(),
        "oppgaveId" to oppgaveId,
    ).singleOrNull { it.uuid("vedtaksperiode_id") }.let { vedtaksperiodeId ->
        asSQL(
            """
            INSERT INTO pa_vent (vedtaksperiode_id, saksbehandler_ref, frist) VALUES (:vedtaksperiodeId, :saksbehandlerRef, :frist)
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "saksbehandlerRef" to saksbehandlerOid,
            "frist" to frist,
        ).update()
    }

    fun slettPåVent(oppgaveId: Long) =
        asSQL(
            """
            SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).singleOrNull { it.uuidOrNull("vedtaksperiode_id") }?.let { vedtaksperiodeId ->
            asSQL(
                """
                DELETE FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
            ).update()
        }

    override fun erPåVent(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT 1 FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { true } ?: false

    fun erPåVent(oppgaveId: Long) =
        asSQL(
            """
            select 1 from pa_vent
            join vedtak v on pa_vent.vedtaksperiode_id = v.vedtaksperiode_id
            join oppgave o on v.id = o.vedtak_ref
            where o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).singleOrNull { true } ?: false
}

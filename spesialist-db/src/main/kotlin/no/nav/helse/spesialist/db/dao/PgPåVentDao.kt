package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.PåVentDao
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PgPåVentDao private constructor(
    private val queryRunner: QueryRunner,
) : PåVentDao, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagrePåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        årsaker: List<PåVentÅrsak>,
        notatTekst: String?,
        dialogRef: Long,
    ) {
        val vedtaksperiodeId =
            asSQL(
                """
                SELECT v.vedtaksperiode_id FROM vedtak v
                INNER JOIN oppgave o ON v.id = o.vedtak_ref
                WHERE o.id = :oppgaveId
                """.trimIndent(),
                "oppgaveId" to oppgaveId,
            ).singleOrNull { it.uuid("vedtaksperiode_id") } ?: return

        asSQL(
            """
            INSERT INTO pa_vent (vedtaksperiode_id, saksbehandler_ref, frist, dialog_ref, notattekst, årsaker)
            VALUES (:vedtaksperiodeId, :saksbehandlerRef, :frist, :dialogRef, :notatTekst, :arsaker::varchar[])
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "saksbehandlerRef" to saksbehandlerOid,
            "frist" to frist,
            "dialogRef" to dialogRef,
            "notatTekst" to notatTekst,
            "arsaker" to årsaker.somDbArray { it.årsak },
        ).update()
    }

    override fun slettPåVent(oppgaveId: Long) =
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

    override fun erPåVent(oppgaveId: Long) =
        asSQL(
            """
            select 1 from pa_vent
            join vedtak v on pa_vent.vedtaksperiode_id = v.vedtaksperiode_id
            join oppgave o on v.id = o.vedtak_ref
            where o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).singleOrNull { true } ?: false

    override fun oppdaterPåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        årsaker: List<PåVentÅrsak>,
        notatTekst: String?,
        dialogRef: Long,
    ) {
        asSQL(
            """
            UPDATE pa_vent SET
              (frist, dialog_ref, saksbehandler_ref, notattekst, årsaker) =
              ( :frist, :dialogRef, :saksbehandlerRef, :notatTekst, :arsaker::varchar[])
              where vedtaksperiode_id = (
                SELECT v.vedtaksperiode_id FROM vedtak v, oppgave o
                  WHERE v.id = o.vedtak_ref
                  AND o.id = :oppgaveId
              )
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
            "saksbehandlerRef" to saksbehandlerOid,
            "frist" to frist,
            "dialogRef" to dialogRef,
            "notatTekst" to notatTekst,
            "arsaker" to årsaker.somDbArray { it.årsak },
        ).update()
    }
}

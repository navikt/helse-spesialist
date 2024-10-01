package no.nav.helse.modell.påvent

import no.nav.helse.HelseDao
import no.nav.helse.db.PåVentRepository
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PåVentDao(dataSource: DataSource) : HelseDao(dataSource), PåVentRepository {
    fun lagrePåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        begrunnelse: String?,
    ) = asSQL(
        """
        SELECT v.vedtaksperiode_id
        FROM vedtak v
        INNER JOIN oppgave o on v.id = o.vedtak_ref
        WHERE o.id = :oppgaveId
        """.trimIndent(),
        mapOf(
            "oppgaveId" to oppgaveId,
        ),
    ).single { it.uuid("vedtaksperiode_id") }.let { vedtaksperiodeId ->
        asSQL(
            """
            INSERT INTO pa_vent (vedtaksperiode_id, saksbehandler_ref, frist, begrunnelse) VALUES (:vedtaksperiodeId, :saksbehandlerRef, :frist, :begrunnelse)
            """.trimIndent(),
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "saksbehandlerRef" to saksbehandlerOid,
                "frist" to frist,
                "begrunnelse" to begrunnelse,
            ),
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
            mapOf(
                "oppgaveId" to oppgaveId,
            ),
        ).single { it.uuidOrNull("vedtaksperiode_id") }?.let { vedtaksperiodeId ->
            asSQL(
                """
                DELETE FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
                """.trimIndent(),
                mapOf("vedtaksperiodeId" to vedtaksperiodeId),
            ).update()
        }

    override fun erPåVent(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT 1 FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent(),
            mapOf("vedtaksperiodeId" to vedtaksperiodeId),
        ).single { it.boolean(1) } ?: false

    fun erPåVent(oppgaveId: Long) =
        asSQL(
            """
            select 1 from pa_vent
            join vedtak v on pa_vent.vedtaksperiode_id = v.vedtaksperiode_id
            join oppgave o on v.id = o.vedtak_ref
            where o.id = :oppgaveId
            """.trimIndent(),
            mapOf("oppgaveId" to oppgaveId),
        ).single { it.boolean(1) } ?: false
}

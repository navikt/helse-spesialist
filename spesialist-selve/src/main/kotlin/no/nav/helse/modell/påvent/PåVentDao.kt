package no.nav.helse.modell.påvent

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.db.PåVentRepository
import no.nav.helse.db.TransactionalPåVentDao
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PåVentDao(private val dataSource: DataSource) : HelseDao(dataSource), PåVentRepository {
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
        mapOf(
            "oppgaveId" to oppgaveId,
        ),
    ).single { it.uuid("vedtaksperiode_id") }.let { vedtaksperiodeId ->
        asSQL(
            """
            INSERT INTO pa_vent (vedtaksperiode_id, saksbehandler_ref, frist) VALUES (:vedtaksperiodeId, :saksbehandlerRef, :frist)
            """.trimIndent(),
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "saksbehandlerRef" to saksbehandlerOid,
                "frist" to frist,
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

    override fun erPåVent(vedtaksperiodeId: UUID): Boolean {
        sessionOf(dataSource).use { session ->
            return TransactionalPåVentDao(session).erPåVent(vedtaksperiodeId)
        }
    }

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

package no.nav.helse.modell.påvent

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class PåVentDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    fun lagrePåVent(oppgaveId: Long, saksbehandlerOid: UUID, frist: LocalDate?, begrunnelse: String?) =
        asSQL(
            """
            SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """.trimIndent(), mapOf(
                "oppgaveId" to oppgaveId
            )
        ).single { it.uuid("vedtaksperiode_id") }.let { vedtaksperiodeId ->
            asSQL(
                """
            INSERT INTO pa_vent (vedtaksperiode_id, saksbehandler_ref, frist, begrunnelse) VALUES (:vedtaksperiodeId, :saksbehandlerRef, :frist, :begrunnelse)
        """.trimIndent(),
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "saksbehandlerRef" to saksbehandlerOid,
                    "frist" to frist,
                    "begrunnelse" to begrunnelse
                )
            ).update()
        }

    fun slettPåVent(oppgaveId: Long) = asSQL(
        """
            SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """.trimIndent(), mapOf(
            "oppgaveId" to oppgaveId
        )
    ).single { it.uuidOrNull("vedtaksperiode_id") }?.let { vedtaksperiodeId ->
        asSQL(
            """
            DELETE FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
        """.trimIndent(), mapOf("vedtaksperiodeId" to vedtaksperiodeId)
        ).update()
    }

    fun erPåVent(vedtaksperiodeId: UUID) = asSQL(
        """
            SELECT 1 FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
        """.trimIndent(), mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { it.boolean(1) } ?: false
}

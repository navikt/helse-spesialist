package no.nav.helse.modell

import java.util.*
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class TotrinnsvurderingDao(private val dataSource: DataSource) {
    internal fun opprett(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
               INSERT INTO totrinnsvurdering (vedtaksperiode_id) 
               VALUES (:vedtaksperiodeId)
            """.trimIndent()

            session.run(queryOf(query, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).asExecute)
        }
    }

    fun settSaksbehandler(vedtaksperiodeId: UUID, saksbehandlerOid: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
               UPDATE totrinnsvurdering SET saksbehandler = :saksbehandlerOid, oppdatert = now()
               WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent()

            session.run(
                queryOf(
                    query,
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId, "saksbehandlerOid" to saksbehandlerOid)
                ).asExecute
            )
        }
    }

    fun settBeslutter(vedtaksperiodeId: UUID, saksbehandlerOid: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
               UPDATE totrinnsvurdering SET beslutter = :saksbehandlerOid, oppdatert = now()
               WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent()

            session.run(
                queryOf(
                    query,
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId, "saksbehandlerOid" to saksbehandlerOid)
                ).asExecute
            )
        }
    }

    fun settErRetur(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
               UPDATE totrinnsvurdering SET er_retur = true, oppdatert = now()
               WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent()

            session.run(
                queryOf(
                    query,
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId)
                ).asExecute
            )
        }
    }

    fun settHåndtertRetur(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
               UPDATE totrinnsvurdering SET er_retur = false, oppdatert = now()
               WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent()

            session.run(
                queryOf(
                    query,
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId)
                ).asExecute
            )
        }
    }

    fun ferdigstill(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
               UPDATE totrinnsvurdering SET utbetaling_id_ref = (
                   SELECT id FROM utbetaling_id ui
                   INNER JOIN vedtaksperiode_utbetaling_id vui ON vui.utbetaling_id = ui.utbetaling_id
                   WHERE vui.vedtaksperiode_id = :vedtaksperiodeId
               ), oppdatert = now()
               WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent()

            session.run(
                queryOf(
                    query,
                    mapOf("vedtaksperiodeId" to vedtaksperiodeId)
                ).asExecute
            )
        }
    }
}

package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import org.intellij.lang.annotations.Language

internal class TransactionalTotrinnsvurderingDao(
    private val transactionalSession: TransactionalSession,
) : TotrinnsvurderingRepository {
    override fun hentAktivTotrinnsvurdering(oppgaveId: Long): TotrinnsvurderingFraDatabase? {
        @Language("PostgreSQL")
        val query =
            """
            SELECT v.vedtaksperiode_id,
                   er_retur,
                   tv.saksbehandler,
                   tv.beslutter,
                   ui.id as utbetaling_id,
                   tv.opprettet,
                   tv.oppdatert
            FROM totrinnsvurdering tv
                     INNER JOIN vedtak v on tv.vedtaksperiode_id = v.vedtaksperiode_id
                     INNER JOIN oppgave o on v.id = o.vedtak_ref
                     LEFT JOIN utbetaling_id ui on ui.id = tv.utbetaling_id_ref
            WHERE o.id = :oppgaveId
              AND utbetaling_id_ref IS NULL
            """.trimIndent()

        return transactionalSession.run(
            queryOf(query, mapOf("oppgaveId" to oppgaveId)).map { row ->
                TotrinnsvurderingFraDatabase(
                    vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                    erRetur = row.boolean("er_retur"),
                    saksbehandler = row.uuidOrNull("saksbehandler"),
                    beslutter = row.uuidOrNull("beslutter"),
                    utbetalingId = row.uuidOrNull("utbetaling_id"),
                    opprettet = row.localDateTime("opprettet"),
                    oppdatert = row.localDateTimeOrNull("oppdatert"),
                )
            }.asSingle,
        )
    }

    override fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase) {
        @Language("PostgreSQL")
        val query =
            """
            UPDATE totrinnsvurdering
            SET saksbehandler     = :saksbehandler,
                beslutter         = :beslutter,
                er_retur          = :er_retur,
                oppdatert         = :oppdatert,
                utbetaling_id_ref = (SELECT id FROM utbetaling_id ui WHERE ui.utbetaling_id = :utbetaling_id)
            WHERE vedtaksperiode_id = :vedtaksperiode_id
              AND utbetaling_id_ref IS NULL
            """.trimIndent()

        transactionalSession.run(
            queryOf(
                query,
                mapOf(
                    "saksbehandler" to totrinnsvurderingFraDatabase.saksbehandler,
                    "beslutter" to totrinnsvurderingFraDatabase.beslutter,
                    "er_retur" to totrinnsvurderingFraDatabase.erRetur,
                    "oppdatert" to totrinnsvurderingFraDatabase.oppdatert,
                    "utbetaling_id" to totrinnsvurderingFraDatabase.utbetalingId,
                    "vedtaksperiode_id" to totrinnsvurderingFraDatabase.vedtaksperiodeId,
                ),
            ).asUpdate,
        )
    }
}
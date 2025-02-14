package no.nav.helse.db

import kotliquery.Query
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import java.util.UUID
import javax.sql.DataSource

class PgTotrinnsvurderingDao private constructor(
    queryRunner: QueryRunner,
) : TotrinnsvurderingDao, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun hentAktivTotrinnsvurdering(oppgaveId: Long): Pair<Long, TotrinnsvurderingFraDatabase>? =
        asSQL(
            """
            SELECT v.vedtaksperiode_id,
                   tv.id,
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
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).singleOrNull { row ->
            row.long("id") to
                TotrinnsvurderingFraDatabase(
                    vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                    erRetur = row.boolean("er_retur"),
                    saksbehandler = row.uuidOrNull("saksbehandler"),
                    beslutter = row.uuidOrNull("beslutter"),
                    utbetalingId = row.uuidOrNull("utbetaling_id"),
                    opprettet = row.localDateTime("opprettet"),
                    oppdatert = row.localDateTimeOrNull("oppdatert"),
                )
        }

    override fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase) {
        asSQL(
            """
            UPDATE totrinnsvurdering
            SET saksbehandler     = :saksbehandler,
                beslutter         = :beslutter,
                er_retur          = :er_retur,
                oppdatert         = :oppdatert,
                utbetaling_id_ref = (SELECT id FROM utbetaling_id ui WHERE ui.utbetaling_id = :utbetaling_id)
            WHERE vedtaksperiode_id = :vedtaksperiode_id
              AND utbetaling_id_ref IS NULL
            """.trimIndent(),
            "saksbehandler" to totrinnsvurderingFraDatabase.saksbehandler,
            "beslutter" to totrinnsvurderingFraDatabase.beslutter,
            "er_retur" to totrinnsvurderingFraDatabase.erRetur,
            "oppdatert" to totrinnsvurderingFraDatabase.oppdatert,
            "utbetaling_id" to totrinnsvurderingFraDatabase.utbetalingId,
            "vedtaksperiode_id" to totrinnsvurderingFraDatabase.vedtaksperiodeId,
        ).update()
    }

    override fun hentAktiv(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT * FROM totrinnsvurdering
            WHERE vedtaksperiode_id = :vedtaksperiodeId
            AND utbetaling_id_ref IS NULL
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).tilTotrinnsvurderingOld()

    private fun Query.tilTotrinnsvurderingOld() =
        singleOrNull { row ->
            TotrinnsvurderingOld(
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                erRetur = row.boolean("er_retur"),
                saksbehandler = row.uuidOrNull("saksbehandler"),
                beslutter = row.uuidOrNull("beslutter"),
                utbetalingIdRef = row.longOrNull("utbetaling_id_ref"),
                opprettet = row.localDateTime("opprettet"),
                oppdatert = row.localDateTimeOrNull("oppdatert"),
            )
        }
}

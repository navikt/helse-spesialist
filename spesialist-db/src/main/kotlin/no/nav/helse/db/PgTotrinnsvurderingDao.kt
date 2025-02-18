package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
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
}

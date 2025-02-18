package no.nav.helse.spesialist.db.dao.api

import kotliquery.Row
import no.nav.helse.db.HelseDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import java.util.UUID
import javax.sql.DataSource

class PgTotrinnsvurderingApiDao internal constructor(dataSource: DataSource) :
    HelseDao(dataSource),
    TotrinnsvurderingApiDao {
        override fun hentAktiv(vedtaksperiodeId: UUID) =
            asSQL(
                """
                SELECT * FROM totrinnsvurdering
                WHERE vedtaksperiode_id = :vedtaksperiodeId
                AND utbetaling_id_ref IS NULL
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
            ).single(::tilTotrinnsvurdering)

        private fun tilTotrinnsvurdering(row: Row) =
            TotrinnsvurderingApiDao.TotrinnsvurderingDto(
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                erRetur = row.boolean("er_retur"),
                saksbehandler = row.uuidOrNull("saksbehandler"),
                beslutter = row.uuidOrNull("beslutter"),
                utbetalingIdRef = row.longOrNull("utbetaling_id_ref"),
                opprettet = row.localDateTime("opprettet"),
                oppdatert = row.localDateTimeOrNull("oppdatert"),
            )
    }

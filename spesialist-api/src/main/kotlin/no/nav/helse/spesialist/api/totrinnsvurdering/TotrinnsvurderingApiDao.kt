package no.nav.helse.spesialist.api.totrinnsvurdering

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import no.nav.helse.HelseDao

class TotrinnsvurderingApiDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun hentAktiv(vedtaksperiodeId: UUID) = asSQL(
        """
            SELECT * FROM totrinnsvurdering
            WHERE vedtaksperiode_id = :vedtaksperiodeId
            AND utbetaling_id_ref IS NULL
        """, mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single(::tilTotrinnsvurdering)

    fun hentAktiv(oppgaveId: Long): TotrinnsvurderingDto? = asSQL(
        """
           SELECT * FROM totrinnsvurdering
           INNER JOIN vedtak v on totrinnsvurdering.vedtaksperiode_id = v.vedtaksperiode_id
           INNER JOIN oppgave o on v.id = o.vedtak_ref
           WHERE o.id = :oppgaveId
           AND utbetaling_id_ref IS NULL
        """, mapOf("oppgaveId" to oppgaveId)
    ).single(::tilTotrinnsvurdering)

    private fun tilTotrinnsvurdering(row: Row) =
        TotrinnsvurderingDto(
            vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
            erRetur = row.boolean("er_retur"),
            saksbehandler = row.uuidOrNull("saksbehandler"),
            beslutter = row.uuidOrNull("beslutter"),
            utbetalingIdRef = row.longOrNull("utbetaling_id_ref"),
            opprettet = row.localDateTime("opprettet"),
            oppdatert = row.localDateTimeOrNull("oppdatert")
        )

    data class TotrinnsvurderingDto(
        val vedtaksperiodeId: UUID,
        val erRetur: Boolean,
        val saksbehandler: UUID?,
        val beslutter: UUID?,
        val utbetalingIdRef: Long?,
        val opprettet: LocalDateTime,
        val oppdatert: LocalDateTime?,
    )
}

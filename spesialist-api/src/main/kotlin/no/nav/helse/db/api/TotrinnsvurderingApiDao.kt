package no.nav.helse.db.api

import java.time.LocalDateTime
import java.util.UUID

interface TotrinnsvurderingApiDao {
    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingDto?

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

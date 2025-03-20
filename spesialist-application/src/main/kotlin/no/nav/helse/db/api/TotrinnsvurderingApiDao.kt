package no.nav.helse.db.api

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import java.time.LocalDateTime
import java.util.UUID

// Kan fjernes når vi tar i bruk ny totrinnsløype i prod
interface TotrinnsvurderingApiDao {
    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingDto?

    data class TotrinnsvurderingDto(
        val vedtaksperiodeId: UUID,
        val saksbehandler: UUID?,
        val beslutter: UUID?,
        val utbetalingIdRef: Long?,
        val tilstand: TotrinnsvurderingTilstand,
        val opprettet: LocalDateTime,
        val oppdatert: LocalDateTime?,
    )
}

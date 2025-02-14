package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import java.time.LocalDateTime
import java.util.UUID

data class TotrinnsvurderingDto(
    val vedtaksperiodeId: UUID,
    val erRetur: Boolean,
    val saksbehandler: SaksbehandlerDto?,
    val beslutter: SaksbehandlerDto?,
    val utbetalingId: UUID?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
)

package no.nav.helse.db

import java.time.LocalDateTime
import java.util.UUID

data class TotrinnsvurderingFraDatabase(
    val vedtaksperiodeId: UUID,
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val utbetalingId: UUID?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
)

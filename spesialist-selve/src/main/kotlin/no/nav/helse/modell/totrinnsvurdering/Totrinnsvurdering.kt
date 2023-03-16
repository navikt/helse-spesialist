package no.nav.helse.modell.totrinnsvurdering

import java.time.LocalDateTime
import java.util.UUID

class Totrinnsvurdering(
    val vedtaksperiodeId: UUID,
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val utbetalingIdRef: Long?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
) {
    fun erBeslutteroppgave(): Boolean = !erRetur && saksbehandler != null
}
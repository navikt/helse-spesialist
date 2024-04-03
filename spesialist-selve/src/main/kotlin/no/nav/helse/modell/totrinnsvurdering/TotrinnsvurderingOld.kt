package no.nav.helse.modell.totrinnsvurdering

import java.time.LocalDateTime
import java.util.UUID

class TotrinnsvurderingOld(
    val vedtaksperiodeId: UUID,
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val utbetalingIdRef: Long?,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
) {
    fun erBeslutteroppgave(): Boolean = !erRetur && saksbehandler != null

    override fun equals(other: Any?): Boolean {
        return this === other || (
            other is TotrinnsvurderingOld &&
                vedtaksperiodeId == other.vedtaksperiodeId &&
                erRetur == other.erRetur &&
                saksbehandler == other.saksbehandler &&
                beslutter == other.beslutter &&
                utbetalingIdRef == other.utbetalingIdRef &&
                opprettet.withNano(0) == other.opprettet.withNano(0) &&
                oppdatert?.withNano(0) == other.oppdatert?.withNano(0)
        )
    }

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + erRetur.hashCode()
        result = 31 * result + (saksbehandler?.hashCode() ?: 0)
        result = 31 * result + (beslutter?.hashCode() ?: 0)
        result = 31 * result + (utbetalingIdRef?.hashCode() ?: 0)
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + (oppdatert?.hashCode() ?: 0)
        return result
    }
}

package no.nav.helse.modell.totrinnsvurdering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.modell.Saksbehandler

class Totrinnsvurdering(
    private val vedtaksperiodeId: UUID,
    private var erRetur: Boolean,
    private var saksbehandler: Saksbehandler?,
    private var beslutter: Saksbehandler?,
    private val utbetalingIdRef: Long?,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime?
) {
    fun erBeslutteroppgave(): Boolean = !erRetur && saksbehandler != null

    fun tidligereBeslutter() = beslutter

    fun opprinneligSaksbehandler() = saksbehandler

    fun sendTilBeslutter(behandlendeSaksbehandler: Saksbehandler) {
        saksbehandler = behandlendeSaksbehandler
        oppdatert = LocalDateTime.now()
        if (erRetur) erRetur = false
    }

    fun sendIRetur(beslutter: Saksbehandler) {
        this.beslutter = beslutter
        oppdatert = LocalDateTime.now()
        erRetur = true
    }

    fun accept(totrinnsvurderingVisitor: TotrinnsvurderingVisitor) {
        totrinnsvurderingVisitor.visitTotrinnsvurdering(vedtaksperiodeId, erRetur, saksbehandler, beslutter, utbetalingIdRef, opprettet, oppdatert)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (
            other is Totrinnsvurdering &&
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
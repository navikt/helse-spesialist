package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

class Totrinnsvurdering(
    private val vedtaksperiodeId: UUID,
    private var erRetur: Boolean,
    private var saksbehandler: Saksbehandler?,
    private var beslutter: Saksbehandler?,
    private var utbetalingId: UUID?,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime?,
) {
    private val erBeslutteroppgave: Boolean get() = !erRetur && saksbehandler != null

    internal fun accept(totrinnsvurderingVisitor: TotrinnsvurderingVisitor) {
        totrinnsvurderingVisitor.visitTotrinnsvurdering(
            vedtaksperiodeId,
            erRetur,
            saksbehandler,
            beslutter,
            utbetalingId,
            opprettet,
            oppdatert,
        )
    }

    internal fun tidligereBeslutter() = beslutter

    internal fun opprinneligSaksbehandler() = saksbehandler

    internal fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: Saksbehandler,
    ) {
        if (erBeslutteroppgave) throw OppgaveAlleredeSendtBeslutter(oppgaveId)
        if (behandlendeSaksbehandler == beslutter) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        saksbehandler = behandlendeSaksbehandler
        oppdatert = LocalDateTime.now()
        if (erRetur) erRetur = false
    }

    internal fun sendIRetur(
        oppgaveId: Long,
        beslutter: Saksbehandler,
    ) {
        if (!erBeslutteroppgave) throw OppgaveAlleredeSendtIRetur(oppgaveId)
        if (beslutter == saksbehandler) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        this.beslutter = beslutter
        oppdatert = LocalDateTime.now()
        erRetur = true
    }

    internal fun ferdigstill(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (
            other is Totrinnsvurdering &&
                vedtaksperiodeId == other.vedtaksperiodeId &&
                erRetur == other.erRetur &&
                saksbehandler == other.saksbehandler &&
                beslutter == other.beslutter &&
                utbetalingId == other.utbetalingId &&
                opprettet.withNano(0) == other.opprettet.withNano(0) &&
                oppdatert?.withNano(0) == other.oppdatert?.withNano(0)
        )
    }

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + erRetur.hashCode()
        result = 31 * result + (saksbehandler?.hashCode() ?: 0)
        result = 31 * result + (beslutter?.hashCode() ?: 0)
        result = 31 * result + (utbetalingId?.hashCode() ?: 0)
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + (oppdatert?.hashCode() ?: 0)
        return result
    }
}

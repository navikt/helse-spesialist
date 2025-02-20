package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class TotrinnsvurderingId(val value: Long)

class Totrinnsvurdering private constructor(
    id: TotrinnsvurderingId?,
    val vedtaksperiodeId: UUID,
    erRetur: Boolean,
    saksbehandler: SaksbehandlerOid?,
    beslutter: SaksbehandlerOid?,
    utbetalingId: UUID?,
    val opprettet: LocalDateTime,
    oppdatert: LocalDateTime?,
    overstyringer: List<Overstyring> = emptyList(),
    ferdigstilt: Boolean = false,
) : AggregateRoot<TotrinnsvurderingId>(id) {
    private val _overstyringer: MutableList<Overstyring> = overstyringer.toMutableList()
    val overstyringer: List<Overstyring>
        get() = _overstyringer

    var erRetur: Boolean = erRetur
        private set

    var saksbehandler: SaksbehandlerOid? = saksbehandler
        private set

    var beslutter: SaksbehandlerOid? = beslutter
        private set

    var utbetalingId: UUID? = utbetalingId
        private set

    var oppdatert: LocalDateTime? = oppdatert
        private set

    var ferdigstilt: Boolean = ferdigstilt
        private set

    val erBeslutteroppgave: Boolean get() = !erRetur && saksbehandler != null

    fun settRetur() =
        oppdatering {
            erRetur = true
        }

    fun settBeslutter(beslutter: SaksbehandlerOid) =

        oppdatering {
            this.beslutter = beslutter
        }

    fun settSaksbehandler(saksbehandlerOid: SaksbehandlerOid) {
        check(saksbehandler == null) { "Forsøker å overskrive satt saskbehandler" }
        oppdatering {
            this.saksbehandler = saksbehandlerOid
        }
    }

    fun nyOverstyring(overstyring: Overstyring) =
        oppdatering {
            _overstyringer.add(overstyring)
        }

    fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: SaksbehandlerOid,
    ) = oppdatering {
        if (erBeslutteroppgave) throw OppgaveAlleredeSendtBeslutter(oppgaveId)
        if (behandlendeSaksbehandler == beslutter) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        saksbehandler = behandlendeSaksbehandler
        if (erRetur) erRetur = false
    }

    fun sendIRetur(
        oppgaveId: Long,
        beslutter: SaksbehandlerOid,
    ) = oppdatering {
        if (!erBeslutteroppgave) throw OppgaveAlleredeSendtIRetur(oppgaveId)
        if (beslutter == saksbehandler) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        this.beslutter = beslutter
        erRetur = true
    }

    fun ferdigstill(
        utbetalingId: UUID,
        skalBenytteNyTotrinnsvurderingsløsning: Boolean = false,
    ) = oppdatering {
        this.utbetalingId = utbetalingId
        this.ferdigstilt = true
        if (!skalBenytteNyTotrinnsvurderingsløsning) {
            this._overstyringer
                .filter {
                    it.vedtaksperiodeId == vedtaksperiodeId ||
                        it.kobledeVedtaksperioder()
                            .contains(vedtaksperiodeId)
                }
                .forEach { it.ferdigstill() }
            return@oppdatering
        }
        this._overstyringer
            .forEach { it.ferdigstill() }
    }

    private fun <T> oppdatering(block: () -> T): T {
        return block().also {
            oppdatert = LocalDateTime.now()
        }
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

    companion object {
        fun ny(vedtaksperiodeId: UUID): Totrinnsvurdering {
            return Totrinnsvurdering(
                id = null,
                vedtaksperiodeId = vedtaksperiodeId,
                erRetur = false,
                saksbehandler = null,
                beslutter = null,
                utbetalingId = null,
                opprettet = LocalDateTime.now(),
                oppdatert = null,
                overstyringer = emptyList(),
                ferdigstilt = false,
            )
        }

        fun fraLagring(
            id: TotrinnsvurderingId,
            vedtaksperiodeId: UUID,
            erRetur: Boolean,
            saksbehandler: SaksbehandlerOid?,
            beslutter: SaksbehandlerOid?,
            utbetalingId: UUID?,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime?,
            overstyringer: List<Overstyring>,
            ferdigstilt: Boolean,
        ): Totrinnsvurdering {
            return Totrinnsvurdering(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                erRetur = erRetur,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                utbetalingId = utbetalingId,
                opprettet = opprettet,
                oppdatert = oppdatert,
                overstyringer = overstyringer,
                ferdigstilt = ferdigstilt,
            )
        }
    }
}

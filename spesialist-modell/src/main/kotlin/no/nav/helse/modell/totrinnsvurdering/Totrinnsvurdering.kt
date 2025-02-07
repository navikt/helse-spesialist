package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.EksisterendeId
import no.nav.helse.modell.Id
import no.nav.helse.modell.NyId
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.gjenopprett
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import java.time.LocalDateTime
import java.util.UUID

class Totrinnsvurdering(
    val id: Id,
    val vedtaksperiodeId: UUID,
    erRetur: Boolean,
    saksbehandler: Saksbehandler?,
    beslutter: Saksbehandler?,
    utbetalingId: UUID?,
    val opprettet: LocalDateTime,
    oppdatert: LocalDateTime?,
    overstyringer: List<Overstyring> = emptyList(),
    ferdigstilt: Boolean = false,
) {
    private val overstyringer: MutableList<Overstyring> = overstyringer.toMutableList()

    var erRetur: Boolean = erRetur
        private set

    var saksbehandler: Saksbehandler? = saksbehandler
        private set

    var beslutter: Saksbehandler? = beslutter
        private set

    var utbetalingId: UUID? = utbetalingId
        private set

    var oppdatert: LocalDateTime? = oppdatert
        private set

    var ferdigstilt: Boolean = ferdigstilt
        private set

    val erBeslutteroppgave: Boolean get() = !erRetur && saksbehandler != null

    fun overstyringer(): List<Overstyring> = overstyringer

    fun ferdigstill() =
        oppdatering {
            ferdigstilt = true
        }

    fun settRetur() =
        oppdatering {
            erRetur = true
        }

    fun settBeslutter(beslutter: Saksbehandler) =
        oppdatering {
            this.beslutter = beslutter
        }

    fun nyOverstyring(overstyring: Overstyring) =
        oppdatering {
            overstyringer.add(overstyring)
        }

    internal fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: Saksbehandler,
    ) = oppdatering {
        if (erBeslutteroppgave) throw OppgaveAlleredeSendtBeslutter(oppgaveId)
        if (behandlendeSaksbehandler == beslutter) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        saksbehandler = behandlendeSaksbehandler
        if (erRetur) erRetur = false
    }

    internal fun sendIRetur(
        oppgaveId: Long,
        beslutter: Saksbehandler,
    ) = oppdatering {
        if (!erBeslutteroppgave) throw OppgaveAlleredeSendtIRetur(oppgaveId)
        if (beslutter == saksbehandler) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        this.beslutter = beslutter
        erRetur = true
    }

    internal fun ferdigstill(utbetalingId: UUID) =
        oppdatering {
            this.utbetalingId = utbetalingId
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
                id = NyId,
                vedtaksperiodeId = vedtaksperiodeId,
                erRetur = false,
                saksbehandler = null,
                beslutter = null,
                utbetalingId = null,
                opprettet = LocalDateTime.now(),
                oppdatert = LocalDateTime.now(),
                overstyringer = emptyList(),
                ferdigstilt = false,
            )
        }

        fun TotrinnsvurderingDto.gjenopprett(
            tilgangskontroll: Tilgangskontroll,
            totrinnsvurderingId: Long?,
        ): Totrinnsvurdering =
            Totrinnsvurdering(
                id = totrinnsvurderingId?.let { EksisterendeId(it) } ?: NyId,
                vedtaksperiodeId = vedtaksperiodeId,
                erRetur = erRetur,
                saksbehandler = saksbehandler?.gjenopprett(tilgangskontroll),
                beslutter = beslutter?.gjenopprett(tilgangskontroll),
                utbetalingId = utbetalingId,
                opprettet = opprettet,
                oppdatert = oppdatert,
            )

        fun Totrinnsvurdering.toDto(): TotrinnsvurderingDto =
            TotrinnsvurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                erRetur = erRetur,
                saksbehandler = saksbehandler?.toDto(),
                beslutter = beslutter?.toDto(),
                utbetalingId = utbetalingId,
                opprettet = opprettet,
                oppdatert = oppdatert,
            )
    }
}

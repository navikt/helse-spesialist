package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.GODKJENT
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.LocalDateTime
import java.util.UUID

enum class TotrinnsvurderingTilstand {
    AVVENTER_SAKSBEHANDLER,
    AVVENTER_BESLUTTER,
    GODKJENT,
}

@JvmInline
value class TotrinnsvurderingId(val value: Long)

class Totrinnsvurdering private constructor(
    id: TotrinnsvurderingId?,
    val fødselsnummer: String,
    saksbehandler: SaksbehandlerOid?,
    beslutter: SaksbehandlerOid?,
    utbetalingId: UUID?,
    val opprettet: LocalDateTime,
    oppdatert: LocalDateTime?,
    overstyringer: List<Overstyring> = emptyList(),
    tilstand: TotrinnsvurderingTilstand,
    vedtaksperiodeForkastet: Boolean,
) : AggregateRoot<TotrinnsvurderingId>(id) {
    private val _overstyringer: MutableList<Overstyring> = overstyringer.toMutableList()
    val overstyringer: List<Overstyring>
        get() = _overstyringer

    var saksbehandler: SaksbehandlerOid? = saksbehandler
        private set

    var beslutter: SaksbehandlerOid? = beslutter
        private set

    var utbetalingId: UUID? = utbetalingId
        private set

    var oppdatert: LocalDateTime? = oppdatert
        private set

    var tilstand: TotrinnsvurderingTilstand = tilstand
        private set

    var vedtaksperiodeForkastet: Boolean = vedtaksperiodeForkastet
        private set

    fun settAvventerSaksbehandler() =
        oppdatering {
            tilstand = AVVENTER_SAKSBEHANDLER
        }

    fun settBeslutter(beslutter: SaksbehandlerOid) =
        oppdatering {
            this.beslutter = beslutter
        }

    fun nyOverstyring(overstyring: Overstyring) =
        oppdatering {
            _overstyringer.add(overstyring)
        }

    fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: SaksbehandlerOid,
    ) = oppdatering {
        if (tilstand == AVVENTER_BESLUTTER) throw OppgaveAlleredeSendtBeslutter(oppgaveId)
        if (behandlendeSaksbehandler == beslutter) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        saksbehandler = behandlendeSaksbehandler
        tilstand = AVVENTER_BESLUTTER
    }

    fun sendIRetur(
        oppgaveId: Long,
        beslutter: SaksbehandlerOid,
    ) = oppdatering {
        if (tilstand != AVVENTER_BESLUTTER) throw OppgaveAlleredeSendtIRetur(oppgaveId)
        if (beslutter == saksbehandler) throw OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)

        this.beslutter = beslutter
        tilstand = AVVENTER_SAKSBEHANDLER
    }

    fun ferdigstill(utbetalingId: UUID) =
        oppdatering {
            this.utbetalingId = utbetalingId
            tilstand = GODKJENT
            this._overstyringer.forEach { it.ferdigstill() }
        }

    fun vedtaksperiodeForkastet() =
        oppdatering {
            if (_overstyringer.isEmpty()) {
                // Teit navn nå som totrinns ikke har vedtaksperiodeId, men dette betyr at alle vedtaksperiodene
                // overstyringene var knyttet til har blitt forkastet.
                vedtaksperiodeForkastet = true
            }
        }

    private fun <T> oppdatering(block: () -> T): T {
        return block().also {
            oppdatert = LocalDateTime.now()
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (
            other is Totrinnsvurdering &&
                tilstand == other.tilstand &&
                saksbehandler == other.saksbehandler &&
                beslutter == other.beslutter &&
                utbetalingId == other.utbetalingId &&
                opprettet.withNano(0) == other.opprettet.withNano(0) &&
                oppdatert?.withNano(0) == other.oppdatert?.withNano(0)
        )
    }

    override fun hashCode(): Int {
        var result = tilstand.hashCode()
        result = 31 * result + (saksbehandler?.hashCode() ?: 0)
        result = 31 * result + (beslutter?.hashCode() ?: 0)
        result = 31 * result + (utbetalingId?.hashCode() ?: 0)
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + (oppdatert?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun ny(fødselsnummer: String): Totrinnsvurdering {
            return Totrinnsvurdering(
                id = null,
                fødselsnummer = fødselsnummer,
                saksbehandler = null,
                beslutter = null,
                utbetalingId = null,
                opprettet = LocalDateTime.now(),
                oppdatert = null,
                overstyringer = emptyList(),
                tilstand = AVVENTER_SAKSBEHANDLER,
                vedtaksperiodeForkastet = false,
            )
        }

        fun fraLagring(
            id: TotrinnsvurderingId,
            fødselsnummer: String,
            saksbehandler: SaksbehandlerOid?,
            beslutter: SaksbehandlerOid?,
            utbetalingId: UUID?,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime?,
            overstyringer: List<Overstyring>,
            tilstand: TotrinnsvurderingTilstand,
            vedtaksperiodeForkastet: Boolean,
        ): Totrinnsvurdering {
            return Totrinnsvurdering(
                id = id,
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                utbetalingId = utbetalingId,
                opprettet = opprettet,
                oppdatert = oppdatert,
                overstyringer = overstyringer,
                tilstand = tilstand,
                vedtaksperiodeForkastet = vedtaksperiodeForkastet,
            )
        }
    }
}

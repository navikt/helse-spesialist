package no.nav.helse.modell

import no.nav.helse.Oppgavestatus
import no.nav.helse.api.OppgaveMediator
import java.util.*

internal class Oppgave private constructor(
    private val navn: String,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID
) {
    private var id: Long? = null
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null

    internal companion object {
        fun avventerSaksbehandler(navn: String, vedtaksperiodeId: UUID) = Oppgave(navn, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId)
    }

    internal fun ferdigstill(id: Long, ident: String, oid: UUID) {
        this.id = id
        status = Oppgavestatus.Ferdigstilt
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    internal fun lagre(oppgaveMediator: OppgaveMediator, hendelseId: UUID, contextId: UUID) {
        id?.also {
            oppgaveMediator.oppdater(hendelseId, contextId, vedtaksperiodeId, it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(hendelseId, contextId, vedtaksperiodeId, navn)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Oppgave) return false
        if (this.id != other.id) return false
        return this.navn == other.navn && this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        return Objects.hash(id, navn, vedtaksperiodeId)
    }
}

package no.nav.helse.modell

import no.nav.helse.mediator.OppgaveMediator
import java.time.LocalDateTime
import java.util.*

internal class Oppgave private constructor(
    private val navn: String,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID
) {
    private var id: Long? = null
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null
    private var tildeling: Pair<UUID, LocalDateTime>? = null

    constructor(id: Long, navn: String, status: Oppgavestatus, vedtaksperiodeId: UUID) : this(navn, status, vedtaksperiodeId) {
        this.id = id
    }

    internal companion object {
        fun avventerSaksbehandler(navn: String, vedtaksperiodeId: UUID) = Oppgave(navn, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId)
    }

    internal fun ferdigstill(ident: String, oid: UUID) {
        status = Oppgavestatus.Ferdigstilt
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    internal fun avventerSystem(ident: String, oid: UUID) {
        status = Oppgavestatus.AvventerSystem
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    internal fun lagre(oppgaveMediator: OppgaveMediator, hendelseId: UUID, contextId: UUID) {
        val oppgaveId = id?.also {
            oppgaveMediator.oppdater(hendelseId, contextId, it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(hendelseId, contextId, vedtaksperiodeId, navn).also { id = it } ?: return
        tildeling?.also {
            oppgaveMediator.tildel(oppgaveId, it)
        }
    }

    internal fun avbryt() {
        if (status == Oppgavestatus.Ferdigstilt) return
        status = Oppgavestatus.Invalidert
    }

    internal fun tildel(saksbehandleroid: UUID, gyldigTil: LocalDateTime) {
        tildeling = saksbehandleroid to gyldigTil
    }

    internal fun tildel(oppgaveMediator: OppgaveMediator, reservasjon: Pair<UUID, LocalDateTime>) {
        oppgaveMediator.tildel(requireNotNull(id), reservasjon)
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

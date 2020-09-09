package no.nav.helse.modell

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.command.OppgaveDao
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

    internal fun lagre(oppgaveDao: OppgaveDao, vedtakDao: VedtakDao, hendelseId: UUID, contextId: UUID) {
        id?.also { oppdater(oppgaveDao, it) } ?: opprett(oppgaveDao, vedtakDao, hendelseId, contextId)
    }

    private fun opprett(oppgaveDao: OppgaveDao, vedtakDao: VedtakDao, hendelseId: UUID, contextId: UUID) {
        val vedtakRef = requireNotNull(vedtakDao.findVedtak(vedtaksperiodeId)?.id)
        oppgaveDao.insertOppgave(
            hendelseId,
            contextId,
            navn,
            status,
            null,
            null,
            vedtakRef
        )
    }

    private fun oppdater(oppgaveDao: OppgaveDao, id: Long) {
        oppgaveDao.updateOppgave(id, status, ferdigstiltAvIdent, ferdigstiltAvOid)
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

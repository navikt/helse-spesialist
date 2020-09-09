package no.nav.helse.api

import no.nav.helse.Oppgavestatus
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import java.util.*

internal class OppgaveMediator(private val oppgaveDao: OppgaveDao,
                               private val vedtakDao: VedtakDao) {

    private val oppgaver = mutableListOf<Oppgave>()

    fun hentOppgaver() = oppgaveDao.hentSaksbehandlerOppgaver()

    fun hentOppgave(fødselsnummer: String) = oppgaveDao.hentSaksbehandlerOppgave(fødselsnummer)

    internal fun oppgave(oppgave: Oppgave) {
        oppgaver.add(oppgave)
    }

    fun lagreOppgaver(hendelse: Hendelse, contextId: UUID) {
        oppgaver
            .onEach { it.lagre(this, hendelse.id, contextId) }
            .clear()
    }

    internal fun opprett(hendelseId: UUID, contextId: UUID, vedtaksperiodeId: UUID, navn: String, status: Oppgavestatus) {
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

    internal fun oppdater(hendelseId: UUID, contextId: UUID, vedtaksperiodeId: UUID, oppgaveId: Long, status: Oppgavestatus, ferdigstiltAvIdent: String?, ferdigstiltAvOid: UUID?) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid)
    }
}

package no.nav.helse.api

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
            .onEach { it.lagre(oppgaveDao, vedtakDao, hendelse.id, contextId) }
            .clear()
    }
}

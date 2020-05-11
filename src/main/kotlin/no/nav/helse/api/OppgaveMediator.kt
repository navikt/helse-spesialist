package no.nav.helse.api

import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto

internal class OppgaveMediator(
    private val oppgaveDao: OppgaveDao
) {
    fun hentOppgaver(): List<SaksbehandleroppgaveDto> {
        return oppgaveDao.findSaksbehandlerOppgaver()
    }
}

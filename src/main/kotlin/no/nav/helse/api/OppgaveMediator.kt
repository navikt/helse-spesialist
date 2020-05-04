package no.nav.helse.api

import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dto.SaksbehandleroppgaveDto

internal class OppgaveMediator(
    private val oppgaveDao: OppgaveDao
) {
    fun hentOppgaver(): List<SaksbehandleroppgaveDto> {
        return oppgaveDao.findSaksbehandlerOppgaver()
    }
}

package no.nav.helse.api

import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.command.OppgaveDto
import no.nav.helse.modell.command.findOppgave
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import javax.sql.DataSource

internal class OppgaveMediator(private val dataSource: DataSource) {
    fun hentOppgaver() = using(sessionOf(dataSource)) { session ->
        session.findSaksbehandlerOppgaver()
    }

    fun hentOppgave(fødselsnummer: String): OppgaveDto? {
        return sessionOf(dataSource).use { it.findOppgave(fødselsnummer) }
    }
}

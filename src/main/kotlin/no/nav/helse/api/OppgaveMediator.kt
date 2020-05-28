package no.nav.helse.api

import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import javax.sql.DataSource

internal class OppgaveMediator(private val dataSource: DataSource) {
    fun hentOppgaver() = using(sessionOf(dataSource)) { session ->
        session.findSaksbehandlerOppgaver()
    }
}

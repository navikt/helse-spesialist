package no.nav.helse.db.api

interface OppgaveApiDao {
    fun finnOppgaveId(fødselsnummer: String): Long?
}

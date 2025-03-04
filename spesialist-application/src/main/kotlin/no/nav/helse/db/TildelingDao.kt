package no.nav.helse.db

interface TildelingDao {
    fun tildelingForPerson(fødselsnummer: String): TildelingDto?

    fun tildelingForOppgave(oppgaveId: Long): TildelingDto?
}

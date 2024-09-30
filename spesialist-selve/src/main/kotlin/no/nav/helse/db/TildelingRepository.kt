package no.nav.helse.db

interface TildelingRepository {
    fun tildelingForPerson(fødselsnummer: String): TildelingDto?

    fun tildelingForOppgave(oppgaveId: Long): TildelingDto?
}

package no.nav.helse.spesialist.domain.oppgave

sealed interface Oppgavehendelse {
    data class OppgaveOppdatert(
        val oppgave: Oppgave,
    ) : Oppgavehendelse

    data class OppgaveOpprettet(
        val oppgave: Oppgave,
    ) : Oppgavehendelse
}

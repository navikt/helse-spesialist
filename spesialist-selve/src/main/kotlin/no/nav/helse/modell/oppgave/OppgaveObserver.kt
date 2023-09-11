package no.nav.helse.modell.oppgave

interface OppgaveObserver {
    fun oppgaveEndret(oppgave: Oppgave) {}
}
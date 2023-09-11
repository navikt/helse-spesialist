package no.nav.helse.modell.oppgave

interface OppgaveObserver {
    fun tilstandEndret(gammelTilstand: Oppgave.Tilstand, nyTilstand: Oppgave.Tilstand, oppgave: Oppgave) {}
}
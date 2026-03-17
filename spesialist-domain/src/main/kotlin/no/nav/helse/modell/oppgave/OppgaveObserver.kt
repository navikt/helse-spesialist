package no.nav.helse.modell.oppgave

import no.nav.helse.spesialist.domain.oppgave.Oppgave

interface OppgaveObserver {
    fun oppgaveEndret(oppgave: Oppgave) {}
}

package no.nav.helse.mediator.oppgave

import no.nav.helse.MeldingPubliserer
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.OppgaveOpprettet
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveObserver

class Oppgavemelder(
    private val fødselsnummer: String,
    private val publiserer: MeldingPubliserer,
) : OppgaveObserver {
    internal fun oppgaveOpprettet(oppgave: Oppgave) {
        publiserer.publiser(fødselsnummer, OppgaveOpprettet(oppgave), "Oppgave opprettet")
    }

    override fun oppgaveEndret(oppgave: Oppgave) {
        publiserer.publiser(fødselsnummer, OppgaveOppdatert(oppgave), "Oppgave endret")
    }
}

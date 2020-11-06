package no.nav.helse.modell.kommando

import no.nav.helse.mediator.OppgaveMediator

internal class AvbrytOppgaverForPersonCommand(
    private val fødselsnummer: String,
    private val oppgaveMediator: OppgaveMediator,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        oppgaveMediator.avbrytOppgaver(fødselsnummer)
        return true
    }

}

package no.nav.helse.modell.kommando

import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.CommandContextDao

internal class AvbrytForPersonCommand(
    fødselsnummer: String,
    oppgaveMediator: OppgaveMediator,
    commandContextDao: CommandContextDao
) : MacroCommand() {

    override val commands: List<Command> = listOf(
        AvbrytOppgaverForPersonCommand(fødselsnummer, oppgaveMediator),
        AvbrytContexterForPersonCommand(fødselsnummer, commandContextDao)
    )
}

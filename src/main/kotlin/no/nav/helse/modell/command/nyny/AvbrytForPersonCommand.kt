package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.command.OppgaveDao

internal class AvbrytForPersonCommand(
    fødselsnummer: String,
    oppgaveDao: OppgaveDao,
    commandContextDao: CommandContextDao
) : MacroCommand() {

    override val commands: List<Command> = listOf(
        AvbrytOppgaverForPersonCommand(fødselsnummer, oppgaveDao),
        AvbrytContexterForPersonCommand(fødselsnummer, commandContextDao)
    )
}

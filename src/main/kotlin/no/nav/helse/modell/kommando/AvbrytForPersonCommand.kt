package no.nav.helse.modell.kommando

import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.OppgaveDao

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

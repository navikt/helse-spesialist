package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.command.OppgaveDao
import java.util.*

internal class AvbrytCommand(
    vedtaksperiodeId: UUID,
    oppgaveDao: OppgaveDao,
    commandContextDao: CommandContextDao
) : MacroCommand() {

    override val commands: List<Command> = listOf(
        AvbrytOppgaveCommand(vedtaksperiodeId, oppgaveDao),
        AvbrytContextCommand(vedtaksperiodeId, commandContextDao)
    )

}

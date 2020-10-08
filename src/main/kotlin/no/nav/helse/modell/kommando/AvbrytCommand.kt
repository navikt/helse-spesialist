package no.nav.helse.modell.kommando

import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.OppgaveDao
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

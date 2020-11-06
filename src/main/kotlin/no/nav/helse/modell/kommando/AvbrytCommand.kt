package no.nav.helse.modell.kommando

import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import java.util.*

internal class AvbrytCommand(
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    oppgaveMediator: OppgaveMediator
) : MacroCommand() {

    override val commands: List<Command> = listOf(
        AvbrytOppgaveCommand(vedtaksperiodeId, oppgaveMediator),
        AvbrytContextCommand(vedtaksperiodeId, commandContextDao)
    )
}

package no.nav.helse.modell.kommando

import no.nav.helse.modell.CommandContextDao
import no.nav.helse.oppgave.OppgaveMediator
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

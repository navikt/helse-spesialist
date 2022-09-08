package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.oppgave.OppgaveMediator

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

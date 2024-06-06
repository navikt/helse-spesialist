package no.nav.helse.modell.kommando

import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.CommandContextDao
import java.util.UUID

internal class AvbrytCommand(
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    oppgaveService: OppgaveService,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            AvbrytOppgaveCommand(vedtaksperiodeId, oppgaveService),
            AvbrytContextCommand(vedtaksperiodeId, commandContextDao),
        )
}

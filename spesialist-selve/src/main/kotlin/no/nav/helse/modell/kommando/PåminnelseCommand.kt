package no.nav.helse.modell.kommando

import no.nav.helse.oppgave.OppgaveMediator
import java.util.*

internal class PåminnelseCommand(
    vedtaksperiodeId: UUID,
    oppgaveMediator: OppgaveMediator
) : MacroCommand() {

    override val commands: List<Command> = listOf(
        FjernGjenliggendeOppgaveCommand(vedtaksperiodeId, oppgaveMediator)
    )

}

package no.nav.helse.modell.kommando

import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.ReservasjonRepository
import no.nav.helse.db.TildelingRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import java.util.UUID

internal class AvbrytCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    oppgaveService: OppgaveService,
    reservasjonRepository: ReservasjonRepository,
    tildelingRepository: TildelingRepository,
    oppgaveRepository: OppgaveRepository,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ReserverPersonHvisTildeltCommand(
                fødselsnummer,
                reservasjonRepository,
                tildelingRepository,
                oppgaveRepository,
                totrinnsvurderingMediator,
            ),
            AvbrytOppgaveCommand(vedtaksperiodeId, oppgaveService),
            AvbrytContextCommand(vedtaksperiodeId, commandContextDao),
        )
}

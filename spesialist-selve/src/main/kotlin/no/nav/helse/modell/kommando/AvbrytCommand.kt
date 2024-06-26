package no.nav.helse.modell.kommando

import no.nav.helse.db.ReservasjonDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import java.util.UUID

internal class AvbrytCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    oppgaveService: OppgaveService,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    oppgaveDao: OppgaveDao,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ReserverPersonHvisTildeltCommand(fødselsnummer, reservasjonDao, tildelingDao, oppgaveDao, totrinnsvurderingMediator),
            AvbrytOppgaveCommand(vedtaksperiodeId, oppgaveService),
            AvbrytContextCommand(vedtaksperiodeId, commandContextDao),
        )
}

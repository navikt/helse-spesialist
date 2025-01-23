package no.nav.helse.modell.kommando

import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import java.util.UUID

internal class AvbrytCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextRepository: CommandContextRepository,
    oppgaveService: OppgaveService,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    oppgaveDao: OppgaveDao,
    totrinnsvurderingService: TotrinnsvurderingService,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ReserverPersonHvisTildeltCommand(
                fødselsnummer = fødselsnummer,
                reservasjonDao = reservasjonDao,
                tildelingDao = tildelingDao,
                oppgaveDao = oppgaveDao,
                totrinnsvurderingService = totrinnsvurderingService,
            ),
            AvbrytOppgaveCommand(vedtaksperiodeId = vedtaksperiodeId, oppgaveService = oppgaveService),
            AvbrytContextCommand(vedtaksperiodeId = vedtaksperiodeId, commandContextRepository = commandContextRepository),
        )
}

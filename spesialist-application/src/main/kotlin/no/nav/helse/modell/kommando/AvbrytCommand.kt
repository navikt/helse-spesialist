package no.nav.helse.modell.kommando

import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.util.UUID

internal class AvbrytCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    oppgaveService: OppgaveService,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ReserverPersonHvisTildeltCommand(
                fødselsnummer = fødselsnummer,
                reservasjonDao = reservasjonDao,
                tildelingDao = tildelingDao,
                totrinnsvurderingRepository = totrinnsvurderingRepository,
            ),
            AvbrytOppgaveCommand(vedtaksperiodeId = vedtaksperiodeId, oppgaveService = oppgaveService),
            AvbrytContextCommand(vedtaksperiodeId = vedtaksperiodeId, commandContextDao = commandContextDao),
        )
}

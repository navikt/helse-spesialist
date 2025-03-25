package no.nav.helse.modell.kommando

import no.nav.helse.FeatureToggles
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.OppgaveDao
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
    oppgaveDao: OppgaveDao,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
    featureToggles: FeatureToggles,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            ReserverPersonHvisTildeltCommand(
                fødselsnummer = fødselsnummer,
                reservasjonDao = reservasjonDao,
                tildelingDao = tildelingDao,
                oppgaveDao = oppgaveDao,
                totrinnsvurderingRepository = totrinnsvurderingRepository,
                featureToggles = featureToggles,
            ),
            AvbrytOppgaveCommand(vedtaksperiodeId = vedtaksperiodeId, oppgaveService = oppgaveService),
            AvbrytTotrinnsvurderingCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                totrinnsvurderingRepository = totrinnsvurderingRepository,
                featureToggles = featureToggles,
            ),
            AvbrytContextCommand(vedtaksperiodeId = vedtaksperiodeId, commandContextDao = commandContextDao),
        )
}

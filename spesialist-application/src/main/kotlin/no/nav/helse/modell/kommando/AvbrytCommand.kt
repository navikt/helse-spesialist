package no.nav.helse.modell.kommando

import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AvbrytCommand(
    fødselsnummer: String,
    vedtaksperiodeId: UUID,
    spleisBehandlingId: SpleisBehandlingId?,
    commandContextDao: CommandContextDao,
    oppgaveService: OppgaveService,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : MacroCommand() {
    private val log = LoggerFactory.getLogger(this::class.java)
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
            ikkesuspenderendeCommand("fjernVedtak") { sessionContext: SessionContext, _: Outbox ->
                if (spleisBehandlingId == null) return@ikkesuspenderendeCommand
                val vedtak = sessionContext.vedtakRepository.finn(spleisBehandlingId) ?: return@ikkesuspenderendeCommand
                if (vedtak.behandletAvSpleis) {
                    log.warn("Spleis har behandlet svar på godkjenningsbehov for perioden, det er merkelig at spesialist behandler godkjenningsbehov etterpå")
                    return@ikkesuspenderendeCommand
                }
                log.info("Sletter vedtak for $spleisBehandlingId")
                sessionContext.vedtakRepository.slett(spleisBehandlingId)
            },
        )
}

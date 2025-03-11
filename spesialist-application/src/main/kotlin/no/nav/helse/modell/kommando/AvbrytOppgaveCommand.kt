package no.nav.helse.modell.kommando

import no.nav.helse.mediator.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AvbrytOppgaveCommand(
    private val vedtaksperiodeId: UUID,
    private val oppgaveService: OppgaveService,
) : Command() {
    private companion object {
        private val log = LoggerFactory.getLogger(AvbrytOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        log.info("invaliderer alle oppgaver relatert til vedtaksperiodeId=$vedtaksperiodeId")
        oppgaveService.avbrytOppgaveFor(vedtaksperiodeId)
        return true
    }
}

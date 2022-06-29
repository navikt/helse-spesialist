package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.api.oppgave.OppgaveMediator
import org.slf4j.LoggerFactory
import java.util.*

internal class AvbrytOppgaveCommand(
    private val vedtaksperiodeId: UUID,
    private val oppgaveMediator: OppgaveMediator
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(AvbrytOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        log.info("invaliderer alle oppgaver relatert til vedtaksperiodeId=$vedtaksperiodeId")
        oppgaveMediator.avbrytOppgaver(vedtaksperiodeId)
        return true
    }

}

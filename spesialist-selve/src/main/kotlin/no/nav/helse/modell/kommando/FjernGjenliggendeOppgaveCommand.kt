package no.nav.helse.modell.kommando

import no.nav.helse.oppgave.OppgaveMediator
import org.slf4j.LoggerFactory
import java.util.*

internal class FjernGjenliggendeOppgaveCommand(
    private val vedtaksperiodeId: UUID,
    private val oppgaveMediator: OppgaveMediator
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(FjernGjenliggendeOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (oppgaveMediator.venterPåSaksbehandler(vedtaksperiodeId)) {
            log.info("Tenker å invalidere oppgave for vedtaksperiode $vedtaksperiodeId")
        }
        return true
    }

}

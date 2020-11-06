package no.nav.helse.modell.kommando

import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.OppgaveDao
import org.slf4j.LoggerFactory
import java.util.*

internal class FerdigstillOppgaveCommand(
    private val oppgaveMediator: OppgaveMediator,
    private val saksbehandlerIdent: String,
    private val oid: UUID,
    private val oppgaveId: Long,
    private val oppgaveDao: OppgaveDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(FerdigstillOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId)) { "Finner ikke oppgave $oppgaveId" }
        log.info("Ferdigstiller saksbehandleroppgave")
        oppgaveMediator.ferdigstill(oppgave, saksbehandlerIdent, oid)
        return true
    }
}

package no.nav.helse.modell.kommando

import no.nav.helse.modell.oppgave.OppgaveDao
import org.slf4j.LoggerFactory

internal class InvaliderSaksbehandlerOppgaveCommand(
    private val fødselsnummer: String,
    private val oppgaveDao: OppgaveDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val antall = oppgaveDao.invaliderOppgaveFor(fødselsnummer)
        sikkerlogger.info("Invaliderte $antall {} for $fødselsnummer", if (antall == 1) "oppgave" else "oppgaver")
        return true
    }

    private companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    }
}

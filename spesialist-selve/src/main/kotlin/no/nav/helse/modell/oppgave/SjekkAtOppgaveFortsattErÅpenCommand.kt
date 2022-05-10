package no.nav.helse.modell.oppgave

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.oppgave.OppgaveDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class SjekkAtOppgaveFortsattErÅpenCommand(
    private val fødselsnummer: String,
    private val oppgaveDao: OppgaveDao,
) : Command {
    private val sikkerLogger: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        val åpenOppgave = oppgaveDao.finnOppgaveId(fødselsnummer)
        if (åpenOppgave == null) {
            sikkerLogger.info("Ingen åpne oppgaver for $fødselsnummer, kommandokjeden ferdigstilles/avsluttes.")
            ferdigstill(context)
        }
        return true
    }
}

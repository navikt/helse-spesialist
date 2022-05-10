package no.nav.helse.modell.oppgave

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill

internal class SjekkAtOppgaveFortsattErÅpenCommand(
    private val fødselsnummer: String,
    private val oppgaveDao: OppgaveDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val åpenOppgave = oppgaveDao.finnOppgaveId(fødselsnummer)
        if (åpenOppgave == null) {
            ferdigstill(context)
        }
        return true
    }
}

package no.nav.helse.modell.oppgave

import no.nav.helse.db.OppgaveDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.spesialist.application.logg.sikkerlogg

internal class SjekkAtOppgaveFortsattErÅpenCommand(
    private val fødselsnummer: String,
    private val oppgaveDao: OppgaveDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val åpenOppgave = oppgaveDao.finnOppgaveId(fødselsnummer)
        if (åpenOppgave == null) {
            sikkerlogg.info("Ingen åpne oppgaver for $fødselsnummer, kommandokjeden ferdigstilles/avsluttes.")
            ferdigstill(context)
        }
        return true
    }
}

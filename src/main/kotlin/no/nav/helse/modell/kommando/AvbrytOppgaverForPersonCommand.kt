package no.nav.helse.modell.kommando

import no.nav.helse.modell.OppgaveDao

internal class AvbrytOppgaverForPersonCommand(
    private val fødselsnummer: String,
    private val oppgaveDao: OppgaveDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        oppgaveDao.invaliderOppgaver(fødselsnummer)
        return true
    }

}

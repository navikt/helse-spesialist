package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.command.OppgaveDao

internal class AvbrytOppgaverForPersonCommand(
    private val fødselsnummer: String,
    private val oppgaveDao: OppgaveDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        oppgaveDao.invaliderOppgaver(fødselsnummer)
        return true
    }

}

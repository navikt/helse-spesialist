package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.saksbehandler.SaksbehandlerDao

internal class InvaliderSaksbehandlerOppgaveCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val saksbehandlerDao: SaksbehandlerDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        saksbehandlerDao.invaliderSaksbehandleroppgaver(fødselsnummer, orgnummer)
        return true
    }
}

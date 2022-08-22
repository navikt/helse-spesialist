package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao

internal class InvaliderSaksbehandlerOppgaveCommand(
    private val fødselsnummer: String,
    private val saksbehandlerDao: SaksbehandlerDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        saksbehandlerDao.invaliderSaksbehandleroppgaver(fødselsnummer)
        return true
    }
}

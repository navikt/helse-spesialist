package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val oppgaveDao: OppgaveDao,
    private val godkjenningMediator: GodkjenningMediator
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (personDao.findAdressebeskyttelse(fødselsnummer) ?.equals(Adressebeskyttelse.StrengtFortrolig) == false )
            return true
        val oppgaveId = oppgaveDao.finnOppgaveId(fødselsnummer) ?: return true

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            context::publiser,
            årsaker,
            oppgaveId
        )
        oppgaveDao.invaliderOppgaveFor(fødselsnummer)
        return true
    }
}

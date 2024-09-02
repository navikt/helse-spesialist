package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortroligUtland

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val oppgaveDao: OppgaveDao,
    private val godkjenningMediator: GodkjenningMediator,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val adressebeskyttelse =
            checkNotNull(personDao.findAdressebeskyttelse(fødselsnummer)) {
                "Forventer at det fins adressebeskyttelse i databasen når denne kommandoen kjører"
            }
        if (adressebeskyttelse !in setOf(StrengtFortrolig, StrengtFortroligUtland)) {
            return true
        }

        val oppgaveId = oppgaveDao.finnOppgaveId(fødselsnummer) ?: return true

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            context::publiser,
            årsaker,
            oppgaveId,
        )
        oppgaveDao.invaliderOppgaveFor(fødselsnummer)
        return true
    }
}

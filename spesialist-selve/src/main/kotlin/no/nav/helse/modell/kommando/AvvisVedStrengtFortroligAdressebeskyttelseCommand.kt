package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortroligUtland

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val personDao: PersonDao,
    private val oppgaveDao: OppgaveDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val godkjenningsbehov: GodkjenningsbehovData,
    private val utbetaling: Utbetaling,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val adressebeskyttelse =
            checkNotNull(personDao.finnAdressebeskyttelse(godkjenningsbehov.fødselsnummer)) {
                "Forventer at det fins adressebeskyttelse i databasen når denne kommandoen kjører"
            }
        if (adressebeskyttelse !in setOf(StrengtFortrolig, StrengtFortroligUtland)) {
            return true
        }

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            publiserer = context::publiser,
            begrunnelser = årsaker,
            utbetaling = utbetaling,
            godkjenningsbehov = godkjenningsbehov,
        )
        oppgaveDao.invaliderOppgaveFor(godkjenningsbehov.fødselsnummer)
        return true
    }
}

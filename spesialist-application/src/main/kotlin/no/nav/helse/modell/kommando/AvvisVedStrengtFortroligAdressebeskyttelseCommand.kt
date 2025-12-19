package no.nav.helse.modell.kommando

import no.nav.helse.db.MeldingDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val identitetsnummer: Identitetsnummer,
    private val godkjenningMediator: GodkjenningMediator,
    private val meldingDao: MeldingDao,
    private val personRepository: PersonRepository,
    private val oppgaveRepository: OppgaveRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val oppgave = oppgaveRepository.finnAktivForPerson(identitetsnummer)
        if (oppgave == null) {
            loggInfo("Ingen aktiv oppgave for personen. Ingenting å avvise")
            return true
        }
        val godkjenningsbehov =
            meldingDao
                .finnSisteGodkjenningsbehov(oppgave.behandlingId)
                ?.data()
                ?: error("Fant ikke godkjenningsbehov")

        val adressebeskyttelse =
            personRepository
                .finn(identitetsnummer)
                ?.info
                ?.adressebeskyttelse
                ?: error("Fant ikke adressebeskyttelse for person")
        if (adressebeskyttelse !in setOf(Adressebeskyttelse.StrengtFortrolig, Adressebeskyttelse.StrengtFortroligUtland)) {
            return true
        }

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            context = context,
            begrunnelser = årsaker,
            behov = godkjenningsbehov,
        )
        oppgave.avbryt()
        oppgaveRepository.lagre(oppgave)
        return true
    }
}

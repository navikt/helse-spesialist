package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val identitetsnummer: Identitetsnummer,
    private val godkjenningMediator: GodkjenningMediator,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val oppgave = sessionContext.oppgaveRepository.finnAktivForPerson(identitetsnummer)
        if (oppgave == null) {
            loggInfo("Ingen aktiv oppgave for personen. Ingenting å avvise")
            return true
        }
        val godkjenningsbehov =
            sessionContext.meldingDao
                .finnSisteGodkjenningsbehov(oppgave.behandlingId.value)
                ?.data()
                ?: error("Fant ikke godkjenningsbehov")

        val adressebeskyttelse =
            sessionContext.personRepository
                .finn(identitetsnummer)
                ?.info
                ?.adressebeskyttelse
                ?: error("Fant ikke adressebeskyttelse for person")
        if (adressebeskyttelse !in setOf(Adressebeskyttelse.StrengtFortrolig, Adressebeskyttelse.StrengtFortroligUtland)) {
            return true
        }

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            begrunnelser = årsaker,
            behov = godkjenningsbehov,
            outbox = outbox,
        )
        oppgave.avbryt()
        sessionContext.oppgaveRepository.lagre(oppgave)
        return true
    }
}

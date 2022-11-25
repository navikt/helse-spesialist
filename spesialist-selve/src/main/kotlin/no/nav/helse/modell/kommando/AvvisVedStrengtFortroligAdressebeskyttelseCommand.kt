package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.slf4j.LoggerFactory

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val oppgaveDao: OppgaveDao,
    private val hendelseDao: HendelseDao,
    private val godkjenningMediator: GodkjenningMediator,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (personDao.findAdressebeskyttelse(fødselsnummer) ?.equals(Adressebeskyttelse.StrengtFortrolig) == false )
            return true
        if (!oppgaveDao.harÅpenOppgave(fødselsnummer))
            return true

        val hendelseId = oppgaveDao.finnGodkjenningsbehov(fødselsnummer)
        val godkjenningsbehov = hendelseDao.finnUtbetalingsgodkjenningbehov(hendelseId)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer)

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            context,
            godkjenningsbehov,
            vedtaksperiodeId,
            fødselsnummer,
            årsaker,
            hendelseId
        )
        oppgaveDao.invaliderOppgaveFor(fødselsnummer)
        return true
    }
}

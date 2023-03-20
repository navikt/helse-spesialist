package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse

internal class AvvisVedStrengtFortroligAdressebeskyttelseCommand(
    private val fødselsnummer: String,
    private val personDao: PersonDao,
    private val oppgaveDao: OppgaveDao,
    private val hendelseDao: HendelseDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetalingDao: UtbetalingDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (personDao.findAdressebeskyttelse(fødselsnummer) ?.equals(Adressebeskyttelse.StrengtFortrolig) == false )
            return true
        val oppgaveId = oppgaveDao.finnOppgaveId(fødselsnummer) ?: return true

        val utbetaling = utbetalingDao.utbetalingFor(oppgaveId)
        val hendelseId = oppgaveDao.finnGodkjenningsbehov(fødselsnummer)
        val godkjenningsbehovJson = hendelseDao.finnUtbetalingsgodkjenningbehovJson(hendelseId)
        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer)

        val årsaker = listOf("Adressebeskyttelse strengt fortrolig")

        godkjenningMediator.automatiskAvvisning(
            context,
            behov,
            vedtaksperiodeId,
            fødselsnummer,
            årsaker,
            hendelseId
        )
        oppgaveDao.invaliderOppgaveFor(fødselsnummer)
        return true
    }
}

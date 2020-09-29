package no.nav.helse.modell.command.nyny

import no.nav.helse.api.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.tildeling.ReservasjonDao
import java.util.*

internal class SaksbehandlerGodkjenningCommand(
    private val fødselsnummer: String,
    vedtaksperiodeId: UUID,
    private val godkjenningsbehovJson: String,
    private val reservasjonDao: ReservasjonDao,
    private val oppgaveMediator: OppgaveMediator
) : Command {
    private val oppgave = Oppgave.avventerSaksbehandler(this::class.java.simpleName, vedtaksperiodeId)

    override fun execute(context: CommandContext): Boolean {
        val reservasjon = reservasjonDao.hentReservasjonFor(fødselsnummer)
        oppgaveMediator.oppgave(oppgave, reservasjon)
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun behandle(context: CommandContext): Boolean {
        val behov = JsonMessage(godkjenningsbehovJson, MessageProblems(godkjenningsbehovJson))
        val løsning = context.get<SaksbehandlerLøsning>() ?: return false
        løsning.ferdigstillOppgave(oppgave, behov)
        oppgaveMediator.oppgave(oppgave, null)
        context.publiser(behov.toJson())
        return true
    }
}

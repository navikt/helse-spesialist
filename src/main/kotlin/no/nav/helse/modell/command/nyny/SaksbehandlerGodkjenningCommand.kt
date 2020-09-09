package no.nav.helse.modell.command.nyny

import no.nav.helse.api.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import java.util.*

internal class SaksbehandlerGodkjenningCommand(
    vedtaksperiodeId: UUID,
    private val godkjenningsbehovJson: String,
    private val oppgaveMediator: OppgaveMediator
) : Command {
    private val oppgave = Oppgave.avventerSaksbehandler(this::class.java.simpleName, vedtaksperiodeId)

    override fun execute(context: CommandContext): Boolean {
        oppgaveMediator.oppgave(oppgave)
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun behandle(context: CommandContext): Boolean {
        val behov = JsonMessage(godkjenningsbehovJson, MessageProblems(godkjenningsbehovJson))
        val løsning = context.get<SaksbehandlerLøsning>() ?: return false
        løsning.ferdigstillOppgave(oppgave, behov)
        oppgaveMediator.oppgave(oppgave)
        context.publiser(behov.toJson())
        return true
    }
}

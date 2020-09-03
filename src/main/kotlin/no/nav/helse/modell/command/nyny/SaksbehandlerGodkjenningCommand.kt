package no.nav.helse.modell.command.nyny

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import java.util.*

internal class SaksbehandlerGodkjenningCommand(
    private val godkjenninghendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val oppgaveDao: OppgaveDao,
    private val vedtakDao: VedtakDao,
    private val godkjenningsbehovJson: String
) : Command {
    override fun execute(context: CommandContext): Boolean {
        opprettOppgave()
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun opprettOppgave() {
        val vedtakRef = requireNotNull(vedtakDao.findVedtak(vedtaksperiodeId)?.id)
        oppgaveDao.insertOppgave(
            eventId = godkjenninghendelseId,
            oppgavetype = this::class.java.simpleName,
            oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
            ferdigstiltAv = null,
            oid = null,
            vedtakRef = vedtakRef
        )
    }

    private fun behandle(context: CommandContext): Boolean {
        val behov = JsonMessage(godkjenningsbehovJson, MessageProblems(godkjenningsbehovJson))
        val løsning = context.get<SaksbehandlerLøsning>() ?: return false
        løsning.ferdigstillOppgave(oppgaveDao, godkjenninghendelseId, this::class.java.simpleName, behov)
        context.publiser(behov.toJson())
        return true
    }
}

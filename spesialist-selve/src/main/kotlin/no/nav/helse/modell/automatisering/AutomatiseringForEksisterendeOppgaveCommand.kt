package no.nav.helse.modell.automatisering

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.oppgave.OppgaveMediator
import org.slf4j.LoggerFactory

internal class AutomatiseringForEksisterendeOppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val oppgaveMediator: OppgaveMediator,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringForEksisterendeOppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            godkjenningMediator.automatiskUtbetaling(context, behov, vedtaksperiodeId, fødselsnummer, hendelseId)
            logg.info("Oppgave avbrytes for vedtaksperiode $vedtaksperiodeId på grunn av automatisering")
            oppgaveMediator.avbrytOppgaver(vedtaksperiodeId)
        }
        return true
    }
}

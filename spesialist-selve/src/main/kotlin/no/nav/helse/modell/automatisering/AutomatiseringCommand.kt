package no.nav.helse.modell.automatisering

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.utbetaling.Utbetaling
import org.slf4j.LoggerFactory

internal class AutomatiseringCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling?,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId) {
            if (utbetaling == null) throw IllegalStateException("Forventer å finne utbetaling for utbetalingId=$utbetalingId, vedtaksperiodeId=$vedtaksperiodeId")
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson, utbetaling)
            godkjenningMediator.automatiskUtbetaling(context, behov, vedtaksperiodeId, fødselsnummer, hendelseId)
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
            ferdigstill(context)
        }

        return true
    }
}

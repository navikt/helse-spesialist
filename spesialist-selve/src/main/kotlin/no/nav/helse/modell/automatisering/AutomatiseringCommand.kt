package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingtype
import org.slf4j.LoggerFactory
import java.util.*

internal class AutomatiseringCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val utbetalingtype: Utbetalingtype,
    private val godkjenningMediator: GodkjenningMediator
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId, utbetalingtype) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            godkjenningMediator.automatiskUtbetaling(context, behov, vedtaksperiodeId, fødselsnummer)
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
        }

        return true
    }
}

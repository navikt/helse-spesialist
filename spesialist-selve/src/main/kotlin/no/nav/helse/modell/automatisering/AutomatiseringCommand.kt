package no.nav.helse.modell.automatisering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.utbetaling.Utbetalingtype
import org.slf4j.LoggerFactory

internal class AutomatiseringCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val godkjenningsbehovJson: String,
    private val utbetalingtype: Utbetalingtype,
    private val godkjenningMediator: GodkjenningMediator,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId, utbetalingtype, periodeFom, periodeTom) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            godkjenningMediator.automatiskUtbetaling(context, behov, vedtaksperiodeId, fødselsnummer, hendelseId)
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
            ferdigstill(context)
        }

        return true
    }
}

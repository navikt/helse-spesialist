package no.nav.helse.modell.utbetaling

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import org.slf4j.LoggerFactory
import java.util.UUID

internal class UtbetalingsfilterCommand(
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val hendelseId: UUID,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetalingsfilter: () -> Utbetalingsfilter
) : Command {

    override fun resume(context: CommandContext) = execute(context)

    override fun execute(context: CommandContext): Boolean {
        val utbetalingsfilter = utbetalingsfilter()
        if (utbetalingsfilter.kanUtbetales) return true

        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
        val årsaker = utbetalingsfilter.årsaker()
        godkjenningMediator.automatiskAvvisning(context, behov, vedtaksperiodeId, fødselsnummer, årsaker, hendelseId)
        sikkerLogg("Automatisk avvisning av vedtaksperiode pga:$årsaker")
        return ferdigstill(context)
    }

    private fun sikkerLogg(melding: String) = sikkerLogg.info(
        melding,
        keyValue("vedtaksperiodeId", "$vedtaksperiodeId"),
        keyValue("fødselsnummer", fødselsnummer),
        keyValue("hendelseId", "$hendelseId")
    )

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}

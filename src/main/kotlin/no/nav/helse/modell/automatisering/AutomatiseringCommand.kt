package no.nav.helse.modell.automatisering

import no.nav.helse.automatiseringsteller
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class AutomatiseringCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    private val godkjenningsbehovJson: String
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiseringCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val kanAutomatiseres = miljøstyrtFeatureToggle.risikovurdering() && miljøstyrtFeatureToggle.automatisering()
            && automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId)

        if (kanAutomatiseres) {
            val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
            behov.løsAutomatisk()
            context.publiser(behov.toJson())
            automatiseringsteller.inc()
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
        }

        automatisering.lagre(kanAutomatiseres, vedtaksperiodeId, hendelseId)
        return true
    }
}

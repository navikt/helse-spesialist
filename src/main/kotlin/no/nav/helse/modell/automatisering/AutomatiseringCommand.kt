package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class AutomatiseringCommand(
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
        val kanAutomatiseres = miljøstyrtFeatureToggle.risikovurdering()
            && automatisering.godkjentForAutomatisertBehandling(vedtaksperiodeId)

        if (kanAutomatiseres) {
            val behov = JsonMessage(godkjenningsbehovJson, MessageProblems(godkjenningsbehovJson))
            behov["@løsning"] = mapOf(
                "Godkjenning" to mapOf(
                    "godkjent" to true,
                    "saksbehandlerIdent" to "SYSTEM",
                    "automatiskBehandling" to true,
                    "godkjenttidspunkt" to LocalDateTime.now()
                )
            )
            context.publiser(behov.toJson())
            logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
        }

        automatisering.lagre(kanAutomatiseres, vedtaksperiodeId, hendelseId)
        return true
    }
}

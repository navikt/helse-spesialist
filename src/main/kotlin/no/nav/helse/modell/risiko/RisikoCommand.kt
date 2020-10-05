package no.nav.helse.modell.risiko

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.kafka.meldinger.RisikovurderingLøsning
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import org.slf4j.LoggerFactory
import java.util.*

internal class RisikoCommand(
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val risikovurderingDao: RisikovurderingDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(RisikoCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.risikovurdering()) return true
        logg.info("Trenger risikovurdering for {}", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        context.behov("Risikovurdering", mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "organisasjonsnummer" to organisasjonsnummer
        ))
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.risikovurdering()) return true
        val løsning = context.get<RisikovurderingLøsning>() ?: return false
        logg.info("Mottok risikovurdering for {}", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        løsning.lagre(risikovurderingDao)
        return true
    }
}

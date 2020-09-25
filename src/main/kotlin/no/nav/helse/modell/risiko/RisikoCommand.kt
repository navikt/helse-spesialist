package no.nav.helse.modell.risiko

import no.nav.helse.mediator.kafka.FeatureToggle
import no.nav.helse.mediator.kafka.meldinger.RisikovurderingLøsning
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import java.util.*

internal class RisikoCommand(
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val risikovurderingDao: RisikovurderingDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (!FeatureToggle.risikovurdering) return true
        context.behov("Risikovurdering", mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "organisasjonsnummer" to organisasjonsnummer
        ))
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        if (!FeatureToggle.risikovurdering) return true
        val løsning = context.get<RisikovurderingLøsning>() ?: return false

        løsning.lagre(risikovurderingDao)
        return true
    }
}

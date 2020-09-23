package no.nav.helse.modell.risiko

import no.nav.helse.mediator.kafka.FeatureToggle
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import java.util.*

internal class RisikoCommand(
    eventId: UUID,
    private val vedtaksperiodeId: UUID,
    private val risikovurderingDao: RisikovurderingDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (!FeatureToggle.risikovurdering) return true
        context.behov("Risikovurdering", mapOf("vedtaksperiodeId" to vedtaksperiodeId))
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        if (!FeatureToggle.risikovurdering) return true
        val løsning = context.get<RisikovurderingDto>() ?: return false

        risikovurderingDao.persisterRisikovurdering(løsning)
        return true
    }
}

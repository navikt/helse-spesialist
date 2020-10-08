package no.nav.helse.modell.kommando

import no.nav.helse.modell.CommandContextDao
import java.util.*

internal class AvbrytContextCommand(
    private val vedtaksperiodeId: UUID,
    private val commandContextDao: CommandContextDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        context.avbryt(commandContextDao, vedtaksperiodeId)
        return true
    }

}

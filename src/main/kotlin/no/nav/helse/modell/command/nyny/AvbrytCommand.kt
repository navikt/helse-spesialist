package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.CommandContextDao
import java.util.*

internal class AvbrytCommand(
    private val vedtaksperiodeId: UUID,
    private val commandContextDao: CommandContextDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        commandContextDao.avbryt(context, vedtaksperiodeId)
        return true
    }
}

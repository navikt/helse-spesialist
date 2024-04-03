package no.nav.helse.modell.kommando

import no.nav.helse.modell.CommandContextDao
import org.slf4j.LoggerFactory
import java.util.*

internal class AvbrytContextCommand(
    private val vedtaksperiodeId: UUID,
    private val commandContextDao: CommandContextDao,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(AvbrytContextCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        log.info("Avbryter alle command contexter knyttet til vedtaksperiodeId=$vedtaksperiodeId")
        context.avbryt(commandContextDao, vedtaksperiodeId)
        return true
    }
}

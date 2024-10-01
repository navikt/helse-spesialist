package no.nav.helse.modell.kommando

import no.nav.helse.db.CommandContextRepository
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AvbrytContextCommand(
    private val vedtaksperiodeId: UUID,
    private val commandContextRepository: CommandContextRepository,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(AvbrytContextCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        log.info("Avbryter alle command contexter knyttet til vedtaksperiodeId=$vedtaksperiodeId")
        context.avbrytAlleForPeriode(commandContextRepository, vedtaksperiodeId)
        return true
    }
}

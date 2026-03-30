package no.nav.helse.modell.kommando

import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AvbrytContextCommand(
    private val vedtaksperiodeId: UUID,
    private val commandContextDao: CommandContextDao,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(AvbrytContextCommand::class.java)
    }

    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        log.info("Avbryter alle command contexter knyttet til vedtaksperiodeId=$vedtaksperiodeId")
        context.avbrytAlleForPeriode(commandContextDao, vedtaksperiodeId)
        return true
    }
}

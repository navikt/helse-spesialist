package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import org.slf4j.LoggerFactory

internal class AvbrytContextCommand(
    private val vedtaksperiodeId: VedtaksperiodeId,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(AvbrytContextCommand::class.java)
    }

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        log.info("Avbryter alle command contexter knyttet til vedtaksperiodeId=${vedtaksperiodeId.value}")
        commandContext.avbrytAlleForPeriode(sessionContext.commandContextDao, vedtaksperiodeId)
        return true
    }
}

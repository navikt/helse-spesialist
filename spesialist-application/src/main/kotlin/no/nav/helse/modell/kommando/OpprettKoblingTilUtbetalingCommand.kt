package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import java.util.UUID

internal class OpprettKoblingTilUtbetalingCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        sessionContext.utbetalingDao.opprettKobling(vedtaksperiodeId, utbetalingId)
        return true
    }
}

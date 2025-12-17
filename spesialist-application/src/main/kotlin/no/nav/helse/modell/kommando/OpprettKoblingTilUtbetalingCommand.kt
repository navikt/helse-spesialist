package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.db.UtbetalingDao
import java.util.UUID

internal class OpprettKoblingTilUtbetalingCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val utbetalingDao: UtbetalingDao,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        utbetalingDao.opprettKobling(vedtaksperiodeId, utbetalingId)
        return true
    }
}

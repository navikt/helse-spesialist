package no.nav.helse.modell.kommando

import no.nav.helse.modell.utbetaling.UtbetalingDao
import java.util.*

internal class OpprettKoblingTilUtbetalingCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val utbetalingDao: UtbetalingDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        utbetalingDao.opprettKobling(vedtaksperiodeId, utbetalingId)
        return true
    }

    override fun undo(context: CommandContext) {
        utbetalingDao.fjernKobling(vedtaksperiodeId, utbetalingId)
    }
}

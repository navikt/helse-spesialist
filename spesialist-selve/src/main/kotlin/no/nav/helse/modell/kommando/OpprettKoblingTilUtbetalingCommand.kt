package no.nav.helse.modell.kommando

import no.nav.helse.db.UtbetalingRepository
import java.util.UUID

internal class OpprettKoblingTilUtbetalingCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val utbetalingRepository: UtbetalingRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        utbetalingRepository.opprettKobling(vedtaksperiodeId, utbetalingId)
        return true
    }

    override fun undo(context: CommandContext) {
        utbetalingRepository.fjernKobling(vedtaksperiodeId, utbetalingId)
    }
}

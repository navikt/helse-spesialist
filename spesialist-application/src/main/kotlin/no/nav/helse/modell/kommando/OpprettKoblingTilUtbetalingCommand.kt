package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId

internal class OpprettKoblingTilUtbetalingCommand(
    private val vedtaksperiodeId: VedtaksperiodeId,
    private val utbetalingId: UtbetalingId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        sessionContext.utbetalingDao.opprettKobling(vedtaksperiodeId.value, utbetalingId.value)
        return true
    }
}

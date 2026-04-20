package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox

internal class OpprettKoblingTilHendelseCommand(
    commandData: GodkjenningsbehovData,
) : Command {
    private val meldingId = commandData.id

    private val vedtaksperiodeId = commandData.vedtaksperiodeId

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        sessionContext.vedtakDao.opprettKobling(vedtaksperiodeId, meldingId)
        return true
    }
}

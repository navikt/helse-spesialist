package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox

internal class OpprettKoblingTilHendelseCommand(
    godkjenningsbehovData: GodkjenningsbehovData,
) : Command {
    private val meldingId = godkjenningsbehovData.id

    private val vedtaksperiodeId = godkjenningsbehovData.vedtaksperiodeId

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        sessionContext.vedtakDao.opprettKobling(vedtaksperiodeId, meldingId)
        return true
    }
}

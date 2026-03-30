package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox

internal class OpprettKoblingTilHendelseCommand(
    commandData: GodkjenningsbehovData,
    private val vedtakDao: VedtakDao,
) : Command {
    private val meldingId = commandData.id

    private val vedtaksperiodeId = commandData.vedtaksperiodeId

    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        vedtakDao.opprettKobling(vedtaksperiodeId, meldingId)
        return true
    }
}

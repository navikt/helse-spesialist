package no.nav.helse.modell.kommando

import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData

internal class OpprettKoblingTilHendelseCommand(
    commandData: GodkjenningsbehovData,
    private val vedtakDao: VedtakDao,
) : Command {
    private val meldingId = commandData.id

    private val vedtaksperiodeId = commandData.vedtaksperiodeId

    override fun execute(context: CommandContext): Boolean {
        vedtakDao.opprettKobling(vedtaksperiodeId, meldingId)
        return true
    }

    override fun undo(context: CommandContext) {
        vedtakDao.fjernKobling(vedtaksperiodeId, meldingId)
    }
}

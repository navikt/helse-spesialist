package no.nav.helse.modell.kommando

import no.nav.helse.modell.VedtakDao
import java.util.*

internal class OpprettKoblingTilHendelseCommand(
    private val hendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val vedtakDao: VedtakDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        vedtakDao.opprettKobling(vedtaksperiodeId, hendelseId)
        return true
    }

    override fun undo(context: CommandContext) {
        vedtakDao.fjernKobling(vedtaksperiodeId, hendelseId)
    }
}

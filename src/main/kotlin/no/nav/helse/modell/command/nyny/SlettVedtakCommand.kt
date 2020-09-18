package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.VedtakDao
import java.util.*

internal class SlettVedtakCommand(
    private val vedtaksperiodeIder: List<UUID>,
    private val vedtakDao: VedtakDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        vedtakDao.fjernVedtaksperioder(vedtaksperiodeIder)
        return true
    }
}

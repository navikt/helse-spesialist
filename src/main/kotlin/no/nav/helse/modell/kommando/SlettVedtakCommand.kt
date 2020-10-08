package no.nav.helse.modell.kommando

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

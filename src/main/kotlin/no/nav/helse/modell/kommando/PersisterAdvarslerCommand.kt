package no.nav.helse.modell.kommando

import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.WarningDto
import java.util.*

internal class PersisterAdvarslerCommand(
    private val vedtaksperiodeId: UUID,
    private val warnings: List<WarningDto>,
    private val vedtakDao: VedtakDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        vedtakDao.leggTilWarnings(vedtaksperiodeId, warnings)
        return true
    }
}

package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.VedtakDao
import java.util.*

internal class PersisterAdvarslerCommand(
    private val godkjenninghendelseId: UUID,
    private val warnings: List<String>,
    private val vedtakDao: VedtakDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        vedtakDao.leggTilWarnings(godkjenninghendelseId, warnings)
        return true
    }
}

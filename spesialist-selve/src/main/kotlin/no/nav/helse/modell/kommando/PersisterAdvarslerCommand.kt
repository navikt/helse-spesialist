package no.nav.helse.modell.kommando

import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.MaybeWarning
import java.util.*

internal class PersisterAdvarslerCommand(
    private val vedtaksperiodeId: UUID,
    private val warnings: List<MaybeWarning>,
    private val warningDao: WarningDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        warningDao.fjernWarnings(vedtaksperiodeId)
        warningDao.leggTilWarnings(vedtaksperiodeId, warnings)
        return true
    }
}

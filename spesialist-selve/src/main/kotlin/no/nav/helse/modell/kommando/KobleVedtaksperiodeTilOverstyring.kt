package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.overstyring.OverstyringDao

internal class KobleVedtaksperiodeTilOverstyring(
    val vedtaksperiodeId: UUID,
    val forårsaketAvId: UUID,
    val overstyringDao: OverstyringDao,
): Command {

    override fun execute(context: CommandContext): Boolean {
        if (overstyringDao.finnesEksternHendelseId(forårsaketAvId)) {
            overstyringDao.kobleOverstyringOgVedtaksperiode(vedtaksperiodeId, forårsaketAvId)
        }

        return true
    }

}
package no.nav.helse.modell.kommando

import no.nav.helse.modell.overstyring.OverstyringDao
import java.util.UUID

internal class OverstyringIgangsattCommand(
    val berørteVedtaksperiodeIder: List<UUID>,
    val kilde: UUID,
    val overstyringDao: OverstyringDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (overstyringDao.finnesEksternHendelseId(kilde)) {
            overstyringDao.kobleOverstyringOgVedtaksperiode(berørteVedtaksperiodeIder, kilde)
        }

        return true
    }
}

package no.nav.helse.modell.kommando

import no.nav.helse.FeatureToggles
import no.nav.helse.db.OverstyringDao
import java.util.UUID

internal class OverstyringIgangsattCommand(
    val berørteVedtaksperiodeIder: List<UUID>,
    val kilde: UUID,
    val overstyringDao: OverstyringDao,
    val featureToggles: FeatureToggles,
) : Command() {
    override fun execute(context: CommandContext): Boolean {
        if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) return true
        if (overstyringDao.finnesEksternHendelseId(kilde)) {
            overstyringDao.kobleOverstyringOgVedtaksperiode(berørteVedtaksperiodeIder, kilde)
        }

        return true
    }
}

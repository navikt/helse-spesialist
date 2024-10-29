package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData

internal class OpprettKoblingTilAvviksvurdering(
    private val commandData: GodkjenningsbehovData,
    private val avviksvurderingDao: AvviksvurderingDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (commandData.avviksvurderingId != null) {
            avviksvurderingDao.opprettKobling(commandData.avviksvurderingId, commandData.vilkårsgrunnlagId)
        }
        return true
    }
}

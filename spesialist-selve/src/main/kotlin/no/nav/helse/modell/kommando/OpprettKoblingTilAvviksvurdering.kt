package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingDao
import java.util.UUID

internal class OpprettKoblingTilAvviksvurdering(
    private val avviksvurderingId: UUID?,
    private val vilkårsgrunnlagId: UUID,
    private val avviksvurderingDao: AvviksvurderingDao
): Command {
    override fun execute(context: CommandContext): Boolean {
        if (avviksvurderingId != null) {
            avviksvurderingDao.opprettKobling(avviksvurderingId, vilkårsgrunnlagId)
        }
        return true
    }
}

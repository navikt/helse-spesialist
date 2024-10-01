package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData

internal class OpprettKoblingTilAvviksvurdering(
    private val commandData: GodkjenningsbehovData,
    private val avviksvurderingRepository: AvviksvurderingRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (commandData.avviksvurderingId != null) {
            avviksvurderingRepository.opprettKobling(commandData.avviksvurderingId, commandData.vilkårsgrunnlagId)
        }
        return true
    }
}

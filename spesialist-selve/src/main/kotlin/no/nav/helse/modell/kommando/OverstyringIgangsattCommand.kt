package no.nav.helse.modell.kommando

import no.nav.helse.db.OverstyringRepository
import java.util.UUID

internal class OverstyringIgangsattCommand(
    val berørteVedtaksperiodeIder: List<UUID>,
    val kilde: UUID,
    val overstyringRepository: OverstyringRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (overstyringRepository.finnesEksternHendelseId(kilde)) {
            overstyringRepository.kobleOverstyringOgVedtaksperiode(berørteVedtaksperiodeIder, kilde)
        }

        return true
    }
}

package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository

internal class VedtaksperiodeGenerasjonCommand(
    val vedtaksperiodeId: UUID,
    val vedtaksperiodeEndretHendelseId: UUID,
    val generasjonRepository: GenerasjonRepository,
    val forrigeTilstand: String,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        generasjonRepository.forsøkOpprett(vedtaksperiodeId, vedtaksperiodeEndretHendelseId)
        return true
    }

}
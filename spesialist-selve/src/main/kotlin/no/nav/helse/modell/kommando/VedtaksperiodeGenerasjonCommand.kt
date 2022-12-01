package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository

internal class VedtaksperiodeGenerasjonCommand(
    private val vedtaksperiodeId: UUID,
    private val vedtaksperiodeEndretHendelseId: UUID,
    private val generasjonRepository: GenerasjonRepository,
    private val forrigeTilstand: String,
    private val gjeldendeTilstand: String,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        if (forrigeTilstand == gjeldendeTilstand) return true
        generasjonRepository.forsøkOpprett(vedtaksperiodeId, vedtaksperiodeEndretHendelseId)
        return true
    }
}
package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository

internal class OpprettFørsteVedtaksperiodeGenerasjonCommand(
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val generasjonRepository: GenerasjonRepository,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        generasjonRepository.opprettFørste(vedtaksperiodeId, hendelseId)
        return true
    }

}
package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext

internal class OpprettKoblingTilGenerasjonCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val generasjonRepository: GenerasjonRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (!Toggle.VedtaksperiodeGenerasjoner.enabled) return true
        val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
        generasjon.håndterNyUtbetaling(utbetalingId)
        return true
    }
}
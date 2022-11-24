package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext

internal class OpprettKoblingTilGenerasjonCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val generasjonRepository: GenerasjonRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        generasjonRepository.utbetalingFor(vedtaksperiodeId, utbetalingId)
        return true
    }
}
package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext

internal class InvaliderUtbetalingForGenerasjonerCommand(
    private val utbetalingId: UUID,
    private val generasjonRepository: GenerasjonRepository
): Command {
    override fun execute(context: CommandContext): Boolean {
        val generasjoner = generasjonRepository.tilhørendeFor(utbetalingId)
        generasjoner.forEach {
            it.invaliderUtbetaling(utbetalingId)
        }
        return true
    }
}
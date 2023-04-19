package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext

internal class OpprettKoblingTilGenerasjonCommand(
    private val hendelseId: UUID,
    private val utbetalingId: UUID,
    private val gjeldendeGenerasjon: Generasjon,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        gjeldendeGenerasjon.håndterNyUtbetaling(hendelseId, utbetalingId)
        return true
    }
}
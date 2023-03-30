package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VarselRepository

internal class OpprettKoblingTilGenerasjonCommand(
    private val hendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val generasjonRepository: GenerasjonRepository,
    private val varselRepository: VarselRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
        generasjon.håndterNyUtbetaling(hendelseId, utbetalingId, varselRepository)
        return true
    }
}
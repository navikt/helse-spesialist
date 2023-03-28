package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon

internal class VedtaksperiodeGenerasjonCommand(
    private val vedtaksperiodeEndretHendelseId: UUID,
    private val varselRepository: VarselRepository,
    private val forrigeTilstand: String,
    private val gjeldendeTilstand: String,
    private val gjeldendeGenerasjon: Generasjon,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (forrigeTilstand == gjeldendeTilstand) return true
        gjeldendeGenerasjon.håndterNyGenerasjon(varselRepository, vedtaksperiodeEndretHendelseId)
        return true
    }
}
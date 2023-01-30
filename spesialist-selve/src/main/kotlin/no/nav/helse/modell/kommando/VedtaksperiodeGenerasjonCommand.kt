package no.nav.helse.modell.kommando

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import org.slf4j.LoggerFactory

internal class VedtaksperiodeGenerasjonCommand(
    private val vedtaksperiodeId: UUID,
    private val vedtaksperiodeEndretHendelseId: UUID,
    private val generasjonRepository: GenerasjonRepository,
    private val varselRepository: VarselRepository,
    private val forrigeTilstand: String,
    private val gjeldendeTilstand: String,
) : Command {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        if (forrigeTilstand == gjeldendeTilstand) return true
        try {
            val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
            generasjon.håndterNyGenerasjon(
                hendelseId = vedtaksperiodeEndretHendelseId,
                varselRepository = varselRepository
            )
        } catch (e: IllegalStateException) {
            sikkerlogg.info(
                """
                Kan ikke opprette ny generasjon for {} fra vedtaksperiode_endret så lenge det ikke eksisterer minimum én generasjon fra før av. 
                Første generasjon kan kun opprettes når vedtaksperioden opprettes.
                """,
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
        }

        return true
    }
}
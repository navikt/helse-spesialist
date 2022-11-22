package no.nav.helse.modell.kommando

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class OpprettFørsteVedtaksperiodeGenerasjonCommand(
    val vedtaksperiodeId: UUID,
    val vedtaksperiodeEndretHendelseId: UUID,
    val generasjonRepository: GenerasjonRepository,
    val forrigeTilstand: String,
) : Command {

    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        generasjonRepository.opprettFørste(vedtaksperiodeId, vedtaksperiodeEndretHendelseId).also {
            sikkerLogg.info(
                "Opprettet ny generasjon = {} for vedtaksperiode = {} på grunn av vedtaksperiode_endret = {}",
                keyValue("generasjonId", it),
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("vedtaksperiodeEndretHendelseId", vedtaksperiodeEndretHendelseId)
            )
        }

        return true
    }

}
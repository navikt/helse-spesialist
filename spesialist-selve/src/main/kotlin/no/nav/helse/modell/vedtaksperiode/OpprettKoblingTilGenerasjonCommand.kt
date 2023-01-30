package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VarselRepository
import org.slf4j.LoggerFactory

internal class OpprettKoblingTilGenerasjonCommand(
    private val hendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val generasjonRepository: GenerasjonRepository,
    private val varselRepository: VarselRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (!Toggle.VedtaksperiodeGenerasjoner.enabled) return true
        val generasjon = try {
            generasjonRepository.sisteFor(vedtaksperiodeId)
        } catch (e: IllegalStateException) {
            sikkerlogg.info(
                "Oppretter generasjon for {} som følge av vedtaksperiode_ny_utbetaling med {}, da vi ikke finner noen eksisterende generasjoner for perioden",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("utbetalingId", utbetalingId),
            )
            generasjonRepository.opprettFørste(vedtaksperiodeId, hendelseId) ?: throw IllegalStateException("Klarte ikke å opprette generasjon for vedtaksperiodeId=$vedtaksperiodeId")
        }

        generasjon.håndterNyUtbetaling(hendelseId, utbetalingId, varselRepository)
        return true
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
package no.nav.helse.modell.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering.Companion.oppdaterSykefraværstilfeller
import org.slf4j.LoggerFactory

internal class OppdaterSykefraværstilfellerCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val vedtaksperioder: List<VedtaksperiodeOppdatering>,
    private val generasjonRepository: GenerasjonRepository,
) : Command {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        sikkerlogg.info(
            "oppdaterer sykefraværstilfeller for {}, {}",
            keyValue("aktørId", aktørId),
            keyValue("fødselsnummer", fødselsnummer)
        )
        vedtaksperioder.oppdaterSykefraværstilfeller(generasjonRepository)
        return true
    }
}
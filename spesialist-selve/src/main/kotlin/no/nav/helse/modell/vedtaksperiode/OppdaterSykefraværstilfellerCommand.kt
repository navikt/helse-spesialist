package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterOppdateringer
import org.slf4j.LoggerFactory

internal class OppdaterSykefraværstilfellerCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val vedtaksperiodeoppdateringer: List<VedtaksperiodeOppdatering>,
    private val generasjoner: List<Generasjon>,
    private val hendelseId: UUID
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
        generasjoner.håndterOppdateringer(vedtaksperiodeoppdateringer, hendelseId)
        return true
    }
}
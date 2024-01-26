package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterNyttVarsel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class NyeVarsler(
    override val id: UUID,
    private val fødselsnummer: String,
    private val varsler: List<Varsel>,
    private val generasjoner: List<Generasjon>,
    private val json: String,
) : Kommandohendelse {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
    override fun execute(context: CommandContext): Boolean {
        generasjoner.håndterNyttVarsel(varsler, id)
        sikkerlogg.info("Lagrer ${varsler.size} varsler for {}", keyValue("fødselsnummer", fødselsnummer))
        return true
    }
}
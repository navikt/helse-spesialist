package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.varsler
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterNyttVarsel
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class NyeVarsler private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    internal val varsler: List<Varsel>,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        varsler = packet["aktiviteter"].varsler(),
        json = packet.toJson()
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
}

internal class NyeVarslerCommand(
    private val id: UUID,
    private val fødselsnummer: String,
    private val generasjoner: List<Generasjon>,
    private val varsler: List<Varsel>
): Command {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        generasjoner.håndterNyttVarsel(varsler, id)
        sikkerlogg.info("Lagrer ${varsler.size} varsler for {}", keyValue("fødselsnummer", fødselsnummer))
        return true
    }
}
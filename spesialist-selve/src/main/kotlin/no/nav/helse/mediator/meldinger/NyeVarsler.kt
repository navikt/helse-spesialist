package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.lagre
import no.nav.helse.modell.varsel.Varsel.Companion.varsler
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class NyeVarsler(
    override val id: UUID,
    private val fødselsnummer: String,
    private val varsler: List<Varsel>,
    private val json: String,
    private val varselRepository: VarselRepository,
    private val generasjonRepository: GenerasjonRepository,
) : Hendelse {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
    override fun execute(context: CommandContext): Boolean {
        varsler.lagre(varselRepository, generasjonRepository)
        sikkerlogg.info("Lagrer ${varsler.size} varsler for {}", keyValue("fødselsnummer", fødselsnummer))
        return true
    }

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
    ) : PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "aktivitetslogg_ny_aktivitet")
                    it.requireKey("@id", "fødselsnummer")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.requireArray("aktiviteter") {
                        requireKey("melding", "nivå", "id")
                        interestedIn("varselkode")
                        require("tidsstempel", JsonNode::asLocalDateTime)
                        requireArray("kontekster") {
                            requireKey("konteksttype", "kontekstmap")
                        }
                    }
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val varsler = packet["aktiviteter"].varsler()

            if (varsler.isEmpty()) return

            sikkerlogg.info(
                "Mottok varsler for {} med {}, {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("hendelseId", hendelseId),
                keyValue("hendelse", packet.toJson())
            )

            mediator.nyeVarsler(hendelseId, fødselsnummer, varsler, packet.toJson(), context)
        }
    }
}
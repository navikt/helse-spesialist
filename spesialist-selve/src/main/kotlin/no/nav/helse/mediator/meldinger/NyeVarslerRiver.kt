package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.varsel.Varsel.Companion.varsler
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class NyeVarslerRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
) : River.PacketListener {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny("@event_name", listOf("aktivitetslogg_ny_aktivitet", "nye_varsler"))
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

        mediator.mottaMelding(NyeVarsler(packet))
    }
}
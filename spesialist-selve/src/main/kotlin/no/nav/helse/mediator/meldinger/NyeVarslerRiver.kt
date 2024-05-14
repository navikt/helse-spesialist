package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.varsler
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class NyeVarslerRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun validations() =
        River.PacketValidation {
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

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        val fødselsnummer = packet["fødselsnummer"].asText()
        val varsler = packet["aktiviteter"].varsler()

        if (varsler.isEmpty()) return

        sikkerlogg.info(
            "Mottok varsler for {} med {}, {}",
            keyValue("fødselsnummer", fødselsnummer),
            keyValue("hendelseId", hendelseId),
            keyValue("hendelse", packet.toJson()),
        )

        mediator.mottaMelding(NyeVarsler(packet), context)
    }
}

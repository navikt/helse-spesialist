package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class NyeVarslerRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandAny("@event_name", listOf("aktivitetslogg_ny_aktivitet", "nye_varsler"))
            it.requireKey("@id", "fødselsnummer")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.require("aktiviteter", inneholderVarslerParser)
            it.requireArray("aktiviteter") {
                requireKey("melding", "nivå", "id")
                require("tidsstempel", JsonNode::asLocalDateTime)
                requireArray("kontekster") {
                    requireKey("konteksttype", "kontekstmap")
                }
            }
        }

    private val inneholderVarslerParser: (JsonNode) -> Any = { node ->
        check((node as ArrayNode).any { element -> element.path("varselkode").isTextual }) { "Ingen av elementene har varselkode." }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(NyeVarsler(packet), context)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        logger.debug("Behandler ikke melding fordi: {}", problems)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NyeVarslerRiver::class.java)
    }
}

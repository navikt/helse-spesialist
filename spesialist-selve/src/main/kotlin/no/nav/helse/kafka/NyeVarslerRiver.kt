package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(NyeVarsler(packet), context)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.debug("Behandler ikke melding fordi: {}", problems)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NyeVarslerRiver::class.java)
    }
}

package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.NyeVarsler

internal class NyeVarslerRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireAny("@event_name", listOf("aktivitetslogg_ny_aktivitet", "nye_varsler"))
            it.require("aktiviteter", inneholderVarslerParser)
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
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
}

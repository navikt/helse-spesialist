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
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.vedtaksperiode.NyeVarsler

class NyeVarslerRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAny("@event_name", listOf("aktivitetslogg_ny_aktivitet", "nye_varsler"))
            it.require("aktiviteter", inneholderVarslerParser)
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
        mediator.mottaMelding(
            NyeVarsler(
                id = packet["@id"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                varsler = packet["aktiviteter"].varsler(),
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }

    private companion object {
        private fun JsonNode.varsler(): List<LegacyVarsel> =
            filter { it["nivå"].asText() == "VARSEL" && it["varselkode"]?.asText() != null }
                .filter { it["kontekster"].any { kontekst -> kontekst["konteksttype"].asText() == "Vedtaksperiode" } }
                .map { jsonNode ->
                    val vedtaksperiodeId =
                        jsonNode["kontekster"]
                            .find { it["konteksttype"].asText() == "Vedtaksperiode" }!!["kontekstmap"]["vedtaksperiodeId"]
                            .asUUID()
                    LegacyVarsel(
                        jsonNode["id"].asUUID(),
                        jsonNode["varselkode"].asText(),
                        jsonNode["tidsstempel"].asLocalDateTime(),
                        vedtaksperiodeId,
                    )
                }
    }
}

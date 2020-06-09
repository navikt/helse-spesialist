package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

class VedtaksperiodeForkastetMessage(
    internal val vedtaksperiodeId: UUID,
    internal val fødselsnummer: String
) {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_forkastet")
                    it.require("@id", ::UUIDFromJsonNode)
                    it.require("vedtaksperiodeId", ::UUIDFromJsonNode)
                    it.requireKey("fødselsnummer")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = UUIDFromJsonNode(packet["vedtaksperiodeId"])
            val fødselsnummer = packet["fødselsnummer"].asText()
            val eventId = UUIDFromJsonNode(packet["@id"])
            spleisbehovMediator.håndter(eventId, VedtaksperiodeForkastetMessage(vedtaksperiodeId, fødselsnummer))
        }
    }
}

private fun UUIDFromJsonNode(jsonNode: JsonNode): UUID = UUID.fromString(jsonNode.asText())

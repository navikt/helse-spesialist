package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

class VedtaksperiodeEndretMessage(
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
                    it.demandValue("@event_name", "vedtaksperiode_endret")
                    it.require("gjeldendeTilstand") { node -> node.isTextual && node.asText() != "TIL_INFOTRYGD" }
                    it.requireKey("vedtaksperiodeId")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val eventId = UUID.fromString(packet["@id"].asText())
            spleisbehovMediator.håndter(eventId, VedtaksperiodeEndretMessage(vedtaksperiodeId, fødselsnummer))
        }
    }
}

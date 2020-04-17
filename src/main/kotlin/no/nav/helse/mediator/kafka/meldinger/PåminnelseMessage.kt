package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

class PåminnelseMessage {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireValue("@event_name", "spesialist_påminnelse")
                    it.requireKey("referanse")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val spleisbehovId = UUID.fromString(packet["referanse"].asText())
            spleisbehovMediator.håndter(spleisbehovId, PåminnelseMessage())
        }

    }
}

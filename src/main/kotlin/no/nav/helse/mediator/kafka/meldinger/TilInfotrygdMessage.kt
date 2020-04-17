package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class TilInfotrygdMessage {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireValue("@event_type", "vedtaksperiode_endret")
                    it.requireKey("vedtaksperiodeId")
                    it.requireValue("gjeldendeTilstand", "TIL_INFOTRYGD")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            spleisbehovMediator.håndter(vedtaksperiodeId, TilInfotrygdMessage())
        }
    }
}

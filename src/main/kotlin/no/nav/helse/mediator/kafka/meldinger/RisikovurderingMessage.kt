package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

class RisikovurderingMessage(
    val vedtaksperiodeId: UUID,
    val samletScore: Int,
    val begrunnelser: List<String>,
    val ufullstendig: Boolean
) {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "risikovurdering")
                    it.requireKey("@id", "vedtaksperiodeId", "samletScore", "ufullstendig")
                    it.requireArray("begrunnelser")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val eventId = UUID.fromString(packet["@id"].asText())
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val samletScore = packet["samletScore"].asInt()
            val begrunnelser = packet["begrunnelser"].map { it.asText() }
            val ufullstendig = packet["ufullstendig"].asBoolean()
            spleisbehovMediator.håndter(
                eventId,
                RisikovurderingMessage(
                    vedtaksperiodeId = vedtaksperiodeId,
                    samletScore = samletScore,
                    begrunnelser = begrunnelser,
                    ufullstendig = ufullstendig
                )
            )
        }
    }
}

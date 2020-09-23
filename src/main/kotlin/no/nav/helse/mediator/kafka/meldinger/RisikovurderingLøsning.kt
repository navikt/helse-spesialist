package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingLøsning(
    val hendelseId: UUID,
    val vedtaksperiodeId: UUID,
    val opprettet: LocalDateTime,
    val samletScore: Int,
    val begrunnelser: List<String>,
    val ufullstendig: Boolean
) {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val hendelseMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "risikovurdering")
                    it.requireKey("@id", "samletScore", "ufullstendig")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.require("vedtaksperiodeId") { message -> UUID.fromString(message.asText()) }
                    it.requireArray("begrunnelser")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            sikkerLogg.info("Mottok melding RisikovurderingMessage: ", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val samletScore = packet["samletScore"].asInt()
            val begrunnelser = packet["begrunnelser"].map { it.asText() }
            val ufullstendig = packet["ufullstendig"].asBoolean()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            hendelseMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                løsning = RisikovurderingLøsning(
                    hendelseId = hendelseId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    opprettet = opprettet,
                    samletScore = samletScore,
                    begrunnelser = begrunnelser,
                    ufullstendig = ufullstendig
                ),
                context = context
            )
        }
    }
}

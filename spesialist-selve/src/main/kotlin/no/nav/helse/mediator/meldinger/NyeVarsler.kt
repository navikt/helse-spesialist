package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class NyeVarsler {

    internal class NyeVarslerRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {

        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireKey("@id")
                    it.demandValue("@event_name", "aktivitetslogg_nye_varsler")
                    it.demandKey("varsler")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.demandKey("fødselsnummer")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Mottok aktivitetslogg_nye_varsler med {}",
                StructuredArguments.keyValue("hendelseId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok aktivitetslogg_nye_varsler med {}, {}",
                StructuredArguments.keyValue("hendelseId", hendelseId),
                StructuredArguments.keyValue("hendelse", packet.toJson()),
            )
            mediator.nyeVarsler(packet)
        }

    }
}
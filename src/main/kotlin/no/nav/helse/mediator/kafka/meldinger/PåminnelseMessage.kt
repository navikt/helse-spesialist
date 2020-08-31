package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class PåminnelseMessage {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "spesialist_påminnelse")
                    it.requireKey("referanse")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke spesialist_påminnelse:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val spleisbehovId = UUID.fromString(packet["referanse"].asText())
            spleisbehovMediator.håndter(spleisbehovId, PåminnelseMessage())
        }

    }
}

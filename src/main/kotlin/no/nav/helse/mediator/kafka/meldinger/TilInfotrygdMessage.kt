package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class TilInfotrygdMessage {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_endret")
                    it.demandValue("gjeldendeTilstand", "TIL_INFOTRYGD")
                    it.requireKey("vedtaksperiodeId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke vedtaksperiode_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            spleisbehovMediator.håndter(vedtaksperiodeId, TilInfotrygdMessage())
        }
    }
}

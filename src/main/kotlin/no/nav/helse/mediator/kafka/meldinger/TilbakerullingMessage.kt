package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class TilbakerullingMessage(
    val fødselsnummer: String,
    val vedtakperioderSlettet: List<UUID>
) {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "person_rullet_tilbake")
                    it.requireKey("fødselsnummer", "vedtaksperioderSlettet")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke person_rullet_tilbake:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val tilbakerullingMessage = TilbakerullingMessage(
                fødselsnummer = packet["fødselsnummer"].asText(),
                vedtakperioderSlettet = packet["vedtaksperioderSlettet"].map { UUID.fromString(it.asText()) }
            )
            spleisbehovMediator.håndter(tilbakerullingMessage)
        }
    }
}

package no.nav.helse.mediator.meldinger.løsninger

import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class HentEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentEnhet"))
                    it.requireKey("@id", "contextId", "hendelseId", "@løsning.HentEnhet")
                }
            }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("forstod ikke HentEnhet:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        mediator.løsning(hendelseId, contextId,
            UUID.fromString(packet["@id"].asText()),
            HentEnhetløsning(packet["@løsning.HentEnhet"].asText()), context)
    }
}
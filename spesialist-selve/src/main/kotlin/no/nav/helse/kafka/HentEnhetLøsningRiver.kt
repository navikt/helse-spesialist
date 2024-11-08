package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class HentEnhetLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("HentEnhet"))
            it.requireKey("@id", "contextId", "hendelseId", "@løsning.HentEnhet")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLog.error("forstod ikke HentEnhet:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        mediator.løsning(
            hendelseId,
            contextId,
            packet["@id"].asUUID(),
            HentEnhetløsning(packet["@løsning.HentEnhet"].asText()),
            context,
        )
    }
}

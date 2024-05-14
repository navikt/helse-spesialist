package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class HentEnhetRiver(
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
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        mediator.løsning(
            hendelseId,
            contextId,
            UUID.fromString(packet["@id"].asText()),
            HentEnhetløsning(packet["@løsning.HentEnhet"].asText()),
            context,
        )
    }
}

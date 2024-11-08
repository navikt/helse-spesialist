package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class EgenAnsattLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("EgenAnsatt"))
            it.demandKey("fødselsnummer")
            it.demandKey("hendelseId")
            it.demandKey("contextId")
            it.requireKey("@id")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.requireKey("@løsning.EgenAnsatt")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerLogg.info("Mottok melding EgenAnsatt:\n{}", packet.toJson())
        val opprettet = packet["@opprettet"].asLocalDateTime()
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()
        val fødselsnummer = packet["fødselsnummer"].asText()

        val erEgenAnsatt = packet["@løsning.EgenAnsatt"].asBoolean()

        val egenAnsattløsning =
            EgenAnsattløsning(
                opprettet = opprettet,
                fødselsnummer = fødselsnummer,
                erEgenAnsatt = erEgenAnsatt,
            )

        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = egenAnsattløsning,
            context = context,
        )
    }
}

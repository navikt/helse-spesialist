package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning

class EgenAnsattLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("EgenAnsatt"))
            it.requireKey("fødselsnummer", "hendelseId", "contextId")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.requireKey("@løsning.EgenAnsatt")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
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

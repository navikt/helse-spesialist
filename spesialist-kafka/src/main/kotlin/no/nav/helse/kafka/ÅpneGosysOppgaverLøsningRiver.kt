package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning

class ÅpneGosysOppgaverLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("ÅpneOppgaver"))
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.requireKey("@id", "contextId", "hendelseId", "fødselsnummer")
            it.require("@løsning.ÅpneOppgaver.antall") {}
            it.requireKey("@løsning.ÅpneOppgaver.oppslagFeilet")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val åpneGosysOppgaver =
            ÅpneGosysOppgaverløsning(
                opprettet = packet["@opprettet"].asLocalDateTime(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                antall = packet["@løsning.ÅpneOppgaver.antall"].takeUnless { it.isMissingOrNull() }?.asInt(),
                oppslagFeilet = packet["@løsning.ÅpneOppgaver.oppslagFeilet"].asBoolean(),
            )

        meldingMediator.løsning(
            hendelseId = packet["hendelseId"].asUUID(),
            contextId = packet["contextId"].asUUID(),
            behovId = packet["@id"].asUUID(),
            løsning = åpneGosysOppgaver,
            publiserer = MessageContextMeldingPubliserer(context = context),
        )
    }
}

package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning

class ArbeidsgiverinformasjonLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Arbeidsgiverinformasjon"))
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("contextId", "hendelseId", "@id")
            it.requireKey("@løsning.Arbeidsgiverinformasjon")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        val løsning = packet["@løsning.Arbeidsgiverinformasjon"]
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning =
                Arbeidsgiverinformasjonløsning(
                    løsning.map { arbeidsgiver ->
                        Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(
                            orgnummer = arbeidsgiver.path("orgnummer").asText(),
                            navn = arbeidsgiver.path("navn").asText(),
                        )
                    },
                ),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }
}

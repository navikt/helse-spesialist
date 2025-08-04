package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID

class KommandokjedePåminnelseRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "kommandokjede_påminnelse")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "commandContextId", "meldingId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.påminnelse(
            meldingId = packet["meldingId"].asUUID(),
            contextId = packet["commandContextId"].asUUID(),
            hendelseId = packet["meldingId"].asUUID(),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }
}

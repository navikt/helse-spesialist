package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import java.util.UUID

class VurderingsmomenterLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Risikovurdering"))
            it.requireKey("contextId", "hendelseId")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.require("Risikovurdering.vedtaksperiodeId") { message -> UUID.fromString(message.asText()) }
            it.requireKey("@løsning.Risikovurdering")
            it.requireKey(
                "@løsning.Risikovurdering.kanGodkjennesAutomatisk",
                "@løsning.Risikovurdering.funn",
                "@løsning.Risikovurdering.kontrollertOk",
            )
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val løsning = packet["@løsning.Risikovurdering"]

        val risikovurdering =
            Risikovurderingløsning(
                vedtaksperiodeId = packet["Risikovurdering.vedtaksperiodeId"].asUUID(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                kanGodkjennesAutomatisk = løsning["kanGodkjennesAutomatisk"].asBoolean(),
                løsning = løsning,
            )

        meldingMediator.løsning(
            hendelseId = packet["hendelseId"].asUUID(),
            contextId = packet["contextId"].asUUID(),
            behovId = packet["@id"].asUUID(),
            løsning = risikovurdering,
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context = context),
        )
    }
}

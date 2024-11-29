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
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderingsmomenterLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Risikovurdering"))
            it.requireKey("contextId", "hendelseId")
        }
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
        sikkerLogg.info("Mottok melding RisikovurderingMessage:\n{}", packet.toJson())
        val opprettet = packet["@opprettet"].asLocalDateTime()
        val vedtaksperiodeId = packet["Risikovurdering.vedtaksperiodeId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()

        val løsning = packet["@løsning.Risikovurdering"]
        val kanGodkjennesAutomatisk = løsning["kanGodkjennesAutomatisk"].asBoolean()

        val risikovurdering =
            Risikovurderingløsning(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                løsning = løsning,
            )

        meldingMediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = risikovurdering,
            context = context,
        )
    }
}

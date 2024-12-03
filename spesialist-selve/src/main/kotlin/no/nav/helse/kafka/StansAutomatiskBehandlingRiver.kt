package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak

internal class StansAutomatiskBehandlingRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "stans_automatisk_behandling")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.requireKey("fødselsnummer")
            it.requireKey("status")
            it.requireKey("årsaker")
            it.requireKey("opprettet")
            it.requireKey("originalMelding")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val id = packet["@id"].asUUID()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val status = packet["status"].asText()
        val årsaker = packet["årsaker"].map { enumValueOf<StoppknappÅrsak>(it.asText()) }.toSet()
        val opprettet = packet["opprettet"].asLocalDateTime()
        val originalMelding = packet["originalMelding"].asText()

        mediator.mottaMelding(
            StansAutomatiskBehandlingMelding(
                id = id,
                fødselsnummer = fødselsnummer,
                status = status,
                årsaker = årsaker,
                opprettet = opprettet,
                originalMelding = originalMelding,
                json = packet.toJson(),
                kilde = "ISYFO",
            ),
            context,
        )
    }
}

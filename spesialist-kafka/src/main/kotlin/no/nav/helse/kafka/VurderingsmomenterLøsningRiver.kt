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
import no.nav.helse.spesialist.application.logg.loggWarn
import tools.jackson.databind.JsonNode
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
            it.require("Risikovurdering.vedtaksperiodeId") { message -> UUID.fromString(message.asString()) }
            it.requireKey("@løsning.Risikovurdering")
            it.requireKey(
                "@løsning.Risikovurdering.kanGodkjennesAutomatisk",
                "@løsning.Risikovurdering.funn",
                "@løsning.Risikovurdering.kontrollertOk",
            )
            it.requireKey("sti")
            it.interestedIn("@løsning.Risikovurdering.andreVedtaksperioder")
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
                andreVedtaksperioder = andreVedtaksperioder(packet["@løsning.Risikovurdering.andreVedtaksperioder"]),
            )

        meldingMediator.løsning(
            hendelseId = packet["hendelseId"].asUUID(),
            contextId = packet["contextId"].asUUID(),
            behovId = packet["@id"].asUUID(),
            løsning = risikovurdering,
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context = context),
            sti = packet["sti"].toList().map { it.asInt() },
        )
    }

    private fun andreVedtaksperioder(node: JsonNode): List<Risikovurderingløsning.AnnenVedtaksperiode> {
        if (!node.isArray) return emptyList()
        return node.mapNotNull { element ->
            val vedtaksperiodeId = element["vedtaksperiodeId"]?.takeIf { it.isString }?.asString()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val kanGodkjennesAutomatisk = element["kanGodkjennesAutomatisk"]?.takeIf { it.isBoolean }?.asBoolean()
            val harFunnOgKontrollertOk = element["funn"]?.isArray == true && element["kontrollertOk"]?.isArray == true
            if (vedtaksperiodeId == null || kanGodkjennesAutomatisk == null || !harFunnOgKontrollertOk) {
                loggWarn("Ignorerer ugyldig element i @løsning.Risikovurdering.andreVedtaksperioder")
                return@mapNotNull null
            }
            Risikovurderingløsning.AnnenVedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                løsning = element,
            )
        }
    }
}

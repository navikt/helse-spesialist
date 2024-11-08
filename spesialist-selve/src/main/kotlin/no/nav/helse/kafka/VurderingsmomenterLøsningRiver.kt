package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderingsmomenterLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("Risikovurdering"))
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.require("Risikovurdering.vedtaksperiodeId") { message -> UUID.fromString(message.asText()) }
            it.demandKey("contextId")
            it.demandKey("hendelseId")
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

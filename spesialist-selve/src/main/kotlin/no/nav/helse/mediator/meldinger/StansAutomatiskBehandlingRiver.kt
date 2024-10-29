package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class StansAutomatiskBehandlingRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "stans_automatisk_behandling")
            it.requireKey("@id")
            it.requireKey("fødselsnummer")
            it.requireKey("status")
            it.requireKey("årsaker")
            it.requireKey("opprettet")
            it.requireKey("originalMelding")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke stoppknapp-melding:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerlogg.info("Mottok melding stans_automatisk_behandling:\n{}", packet.toJson())

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

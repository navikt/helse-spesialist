package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.vergemal.Vergemål
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.util.UUID

internal class Vergemålløsning(
    val vergemål: Vergemål,
) {
    internal class VergemålRiver(
        private val meldingMediator: MeldingMediator,
    ) : SpesialistRiver {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        override fun validations() =
            River.PacketValidation {
                it.demandValue("@event_name", "behov")
                it.demandValue("@final", true)
                it.demandAll("@behov", listOf("Vergemål"))
                it.demandKey("contextId")
                it.demandKey("hendelseId")
                it.demandKey("fødselsnummer")
                it.requireKey("@id")
                it.require("@opprettet") { node -> node.asLocalDateTime() }
                it.requireKey("@løsning.Vergemål")
            }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            sikkerLogg.info("Mottok melding Vergemål:\n{}", packet.toJson())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())

            val vergemålNode = packet["@løsning.Vergemål"]
            val harVergemål = !vergemålNode["vergemål"].isEmpty
            val harFremtidsfullmakter = !vergemålNode["fremtidsfullmakter"].isEmpty
            val harFullmakter = !vergemålNode["fullmakter"].isEmpty

            val vergemål =
                Vergemål(
                    harVergemål = harVergemål,
                    harFremtidsfullmakter = harFremtidsfullmakter,
                    harFullmakter = harFullmakter,
                )

            val vergemålLøsning =
                Vergemålløsning(
                    vergemål = vergemål,
                )

            meldingMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = vergemålLøsning,
                context = context,
            )
        }
    }
}

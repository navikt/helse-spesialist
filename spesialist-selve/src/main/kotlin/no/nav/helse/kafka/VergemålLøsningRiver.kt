package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.vergemal.VergemålOgFremtidsfullmakt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class VergemålLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("Vergemål"))
            it.demandKey("hendelseId")
            it.demandKey("contextId")
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
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()

        val vergemålNode = packet["@løsning.Vergemål"]
        val harVergemål = !vergemålNode["vergemål"].isEmpty
        val harFremtidsfullmakter = !vergemålNode["fremtidsfullmakter"].isEmpty

        val vergemålOgFremtidsfullmakt =
            VergemålOgFremtidsfullmakt(
                harVergemål = harVergemål,
                harFremtidsfullmakter = harFremtidsfullmakter,
            )

        val vergemålLøsning =
            Vergemålløsning(
                vergemålOgFremtidsfullmakt = vergemålOgFremtidsfullmakt,
            )

        meldingMediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = vergemålLøsning,
            context = context,
        )
    }
}

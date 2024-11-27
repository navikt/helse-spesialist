package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.isSameOrAfter
import no.nav.helse.mediator.meldinger.løsninger.isSameOrBefore
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class FullmaktLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("Fullmakt"))
            it.demandKey("contextId")
            it.demandKey("hendelseId")
            it.demandKey("fødselsnummer")
            it.requireKey("@id")
            it.require("@opprettet") { node -> node.asLocalDateTime() }
            it.requireArray("@løsning.Fullmakt") {
                interestedIn("gyldigFraOgMed", "gyldigTilOgMed")
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerLogg.info("Mottok melding Fullmakt:\n{}", packet.toJson())
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()

        val nå = LocalDate.now()
        val harFullmakt =
            packet["@løsning.Fullmakt"].any { fullmaktNode ->
                fullmaktNode["gyldigFraOgMed"].asLocalDate().isSameOrBefore(nå) &&
                    fullmaktNode["gyldigTilOgMed"].asOptionalLocalDate()?.isSameOrAfter(nå) ?: true
            }

        val fullmaktløsning =
            Fullmaktløsning(
                harFullmakt = harFullmakt,
            )

        meldingMediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = fullmaktløsning,
            context = context,
        )
    }
}

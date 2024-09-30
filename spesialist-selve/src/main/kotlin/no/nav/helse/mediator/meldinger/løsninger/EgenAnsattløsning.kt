package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class EgenAnsattløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erEgenAnsatt: Boolean,
) {
    internal fun lagre(egenAnsattRepository: EgenAnsattRepository) {
        egenAnsattRepository.lagre(fødselsnummer, erEgenAnsatt, opprettet)
    }

    internal class EgenAnsattRiver(
        private val mediator: MeldingMediator,
    ) : SpesialistRiver {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        override fun validations() =
            River.PacketValidation {
                it.demandValue("@event_name", "behov")
                it.demandValue("@final", true)
                it.demandAll("@behov", listOf("EgenAnsatt"))
                it.demandKey("fødselsnummer")
                it.demandKey("hendelseId")
                it.demandKey("contextId")
                it.requireKey("@id")
                it.require("@opprettet") { message -> message.asLocalDateTime() }
                it.requireKey("@løsning.EgenAnsatt")
            }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            sikkerLogg.info("Mottok melding EgenAnsatt:\n{}", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val erEgenAnsatt = packet["@løsning.EgenAnsatt"].asBoolean()

            val egenAnsattløsning =
                EgenAnsattløsning(
                    opprettet = opprettet,
                    fødselsnummer = fødselsnummer,
                    erEgenAnsatt = erEgenAnsatt,
                )

            mediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = egenAnsattløsning,
                context = context,
            )
        }
    }
}

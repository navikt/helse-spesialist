package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class EgenAnsattløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erEgenAnsatt: Boolean
) {
    internal fun lagre(egenAnsattDao: EgenAnsattDao) {
        egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, opprettet)
    }

    internal fun evaluer() = erEgenAnsatt

    internal class EgenAnsattRiver(
        rapidsConnection: RapidsConnection,
        private val hendelseMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireKey("@id")
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("EgenAnsatt"))
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.demandKey("contextId")
                    it.demandKey("hendelseId")
                    it.demandKey("fødselsnummer")
                    it.requireKey("@løsning.EgenAnsatt")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding EgenAnsatt: ", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val erEgenAnsatt = packet["@løsning.EgenAnsatt"].asBoolean()

            val egenAnsattløsning = EgenAnsattløsning(
                opprettet = opprettet,
                fødselsnummer = fødselsnummer,
                erEgenAnsatt = erEgenAnsatt
            )

            hendelseMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = egenAnsattløsning,
                context = context
            )
        }
    }
}

package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class DigitalKontaktinformasjonLøsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erDigital: Boolean
) {
    internal fun lagre(digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao) {
        digitalKontaktinformasjonDao.persisterDigitalKontaktinformasjon(
            DigitalKontaktinformasjonDto(
                fødselsnummer = fødselsnummer,
                erDigital = erDigital,
                opprettet = opprettet
            )
        )
    }

    internal class DigitalKontaktinformasjonRiver(
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
                    it.demandAll("@behov", listOf("DigitalKontaktinformasjon"))
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.demandKey("contextId")
                    it.demandKey("hendelseId")
                    it.requireKey("@løsning.DigitalKontaktinformasjon.erDigital")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            sikkerLogg.info("Mottok melding DigitalKontaktinformasjonMessage: ", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val løsning = packet["@løsning.DigitalKontaktinformasjon"]
            val erDigital = løsning["erDigital"].asBoolean()

            val digitalKontaktinformasjon = DigitalKontaktinformasjonLøsning(
                opprettet = opprettet,
                fødselsnummer = fødselsnummer,
                erDigital = erDigital
            )

            hendelseMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                løsning = digitalKontaktinformasjon,
                context = context
            )
        }
    }
}

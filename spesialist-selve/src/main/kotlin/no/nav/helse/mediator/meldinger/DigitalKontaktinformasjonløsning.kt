package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.rapids_rivers.*
import no.nav.helse.warningteller
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class DigitalKontaktinformasjonløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erDigital: Boolean
) {
    internal fun lagre(digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao) {
        digitalKontaktinformasjonDao.lagre(fødselsnummer, erDigital, opprettet)
    }

    internal fun evaluer(warningDao: WarningDao, vedtaksperiodeId: UUID) {
        if (erDigital) return
        val melding =
            "Ikke registrert eller mangler samtykke i Kontakt- og reservasjonsregisteret, eventuell kommunikasjon må skje i brevform"
        warningDao.leggTilWarning(
            vedtaksperiodeId,
            Warning.warning(
                melding,
                WarningKilde.Spesialist
            )
        )
        warningteller.labels("WARN", melding).inc()
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
                    it.demandKey("fødselsnummer")
                    it.requireKey("@løsning.DigitalKontaktinformasjon.erDigital")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding DigitalKontaktinformasjonMessage: ", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val erDigital = packet["@løsning.DigitalKontaktinformasjon.erDigital"].asBoolean()

            val digitalKontaktinformasjon = DigitalKontaktinformasjonløsning(
                opprettet = opprettet,
                fødselsnummer = fødselsnummer,
                erDigital = erDigital
            )

            hendelseMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = digitalKontaktinformasjon,
                context = context
            )
        }
    }
}

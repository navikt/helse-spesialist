package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vergemal.Vergemål
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.util.*

internal class Vergemålløsning(
    private val fødselsnummer: String,
    val vergemål: Vergemål,
) {
    internal fun lagre(vergemålDao: VergemålDao) {
        vergemålDao.lagre(fødselsnummer, vergemål)
    }

    internal class VergemålRiver(
        rapidsConnection: RapidsConnection,
        private val meldingMediator: MeldingMediator
    ) : River.PacketListener {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireKey("@id")
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("Vergemål"))
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.demandKey("contextId")
                    it.demandKey("hendelseId")
                    it.demandKey("fødselsnummer")
                    it.requireKey("@løsning.Vergemål")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding Vergemål:\n{}", packet.toJson())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val vergemålNode = packet["@løsning.Vergemål"]
            val harVergemål = !vergemålNode["vergemål"].isEmpty
            val harFremtidsfullmakter = !vergemålNode["fremtidsfullmakter"].isEmpty
            val harFullmakter = !vergemålNode["fullmakter"].isEmpty

            val vergemål = Vergemål(
                harVergemål = harVergemål,
                harFremtidsfullmakter = harFremtidsfullmakter,
                harFullmakter = harFullmakter
            )

            val vergemålLøsning = Vergemålløsning(
                fødselsnummer = fødselsnummer,
                vergemål = vergemål
            )

            meldingMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = vergemålLøsning,
                context = context
            )
        }
    }
}

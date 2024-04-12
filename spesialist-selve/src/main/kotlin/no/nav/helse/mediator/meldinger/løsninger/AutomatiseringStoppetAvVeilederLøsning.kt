package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class AutomatiseringStoppetAvVeilederLøsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val automatiseringErStoppet: Boolean,
    private val årsaker: List<String>,
) {
    internal class AutomatiseringStoppetAvVeilederRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: MeldingMediator,
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("AutomatiseringStoppetAvVeileder"))
                    it.demandKey("fødselsnummer")
                    it.demandKey("hendelseId")
                    it.demandKey("contextId")
                    it.requireKey("@id")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.requireKey("@løsning.automatiseringStoppet", "@løsning.årsaker")
                }
            }.register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            sikkerLogg.info("Mottok melding AutomatiseringStoppetAvVeileder:\n{}", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()

            val automatiseringErStoppet = packet["@løsning.automatiseringStoppet"].asBoolean()
            val årsaker = packet["@løsning.årsaker"].map { it.asText() }

            val automatiseringStoppetAvVeilederLøsning =
                AutomatiseringStoppetAvVeilederLøsning(
                    opprettet = opprettet,
                    fødselsnummer = fødselsnummer,
                    automatiseringErStoppet = automatiseringErStoppet,
                    årsaker = årsaker,
                )
            mediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = automatiseringStoppetAvVeilederLøsning,
                context = context,
            )
        }

        companion object {
            private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        }
    }
}

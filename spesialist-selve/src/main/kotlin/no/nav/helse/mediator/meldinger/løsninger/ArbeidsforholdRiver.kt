package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import org.slf4j.LoggerFactory
import java.util.UUID

internal class ArbeidsforholdRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "Arbeidsforhold"

    init {
        River(rapidsConnection)
            .apply {
                validate { message ->
                    message.demandValue("@event_name", "behov")
                    message.demandValue("@final", true)
                    message.demandAll("@behov", listOf(behov))
                    message.requireKey(
                        "contextId",
                        "hendelseId",
                        "@id",
                        "@løsning.$behov",
                    )
                }
            }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = UUID.fromString(packet["@id"].asText()),
            løsning = packet.toArbeidsforholdløsninger(),
            context = context,
        )
    }

    private fun JsonMessage.toArbeidsforholdløsninger(): Arbeidsforholdløsning {
        val løsninger = this["@løsning.$behov"].map(::toArbeidsforholdløsning)

        if (løsninger.isEmpty()) {
            sikkerLog.info("Ingen arbeidsforhold i løsningen.\n${this.toJson()}")
        }
        return Arbeidsforholdløsning(løsninger)
    }

    private fun toArbeidsforholdløsning(løsning: JsonNode): Arbeidsforholdløsning.Løsning =
        Arbeidsforholdløsning.Løsning(
            løsning["startdato"].asLocalDate(),
            løsning["sluttdato"].asOptionalLocalDate(),
            løsning["stillingstittel"].asText(),
            løsning["stillingsprosent"].asInt(),
        )
}

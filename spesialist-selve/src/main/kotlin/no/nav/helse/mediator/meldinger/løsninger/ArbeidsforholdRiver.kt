package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import org.slf4j.LoggerFactory

internal class ArbeidsforholdRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "Arbeidsforhold"

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf(behov))
            it.requireKey(
                "contextId",
                "hendelseId",
                "@id",
                "@løsning.$behov",
            )
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
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
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

package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import org.slf4j.LoggerFactory

internal class ArbeidsforholdLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "Arbeidsforhold"

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf(behov))
        }
    }

    override fun validations() =
        River.PacketValidation {
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
        metadata: MessageMetadata,
    ) {
        sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
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

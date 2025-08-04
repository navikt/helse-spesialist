package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import org.slf4j.LoggerFactory

class ArbeidsforholdLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Arbeidsforhold"))
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "contextId",
                "hendelseId",
                "@id",
                "@løsning.Arbeidsforhold",
            )
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
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context = context),
        )
    }

    private fun JsonMessage.toArbeidsforholdløsninger(): Arbeidsforholdløsning {
        val løsninger = this["@løsning.Arbeidsforhold"].map(::toArbeidsforholdløsning)

        if (løsninger.isEmpty()) {
            sikkerlogg.info("Ingen arbeidsforhold i løsningen.\n${this.toJson()}")
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

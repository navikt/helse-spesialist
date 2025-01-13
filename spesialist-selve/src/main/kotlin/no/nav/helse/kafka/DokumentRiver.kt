package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import org.slf4j.LoggerFactory

class DokumentRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "hent-dokument")
            it.requireKey("@løsning.dokument")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "dokumentId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val dokumentId = packet["dokumentId"].asUUID()
        val dokument = packet["@løsning.dokument"]

        sikkerlogg.info(
            "Mottok hendelse hent-dokument og oppdaterer databasen for {} {}",
            StructuredArguments.kv("fødselsnummer", fødselsnummer),
            StructuredArguments.kv("dokumentId", dokumentId),
        )

        meldingMediator.mottaDokument(fødselsnummer, dokumentId, dokument)
    }
}

package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.hendelser.VarseldefinisjonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VarseldefinisjonRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "varselkode_ny_definisjon")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.requireKey("varselkode")
            it.requireKey("gjeldende_definisjon")
            it.requireKey(
                "gjeldende_definisjon.id",
                "gjeldende_definisjon.kode",
                "gjeldende_definisjon.tittel",
                "gjeldende_definisjon.avviklet",
                "gjeldende_definisjon.opprettet",
            )
            it.interestedIn("gjeldende_definisjon.forklaring", "gjeldende_definisjon.handling")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        sikkerlogg.info("Mottok melding om ny definisjon for {}", kv("varselkode", packet["varselkode"].asText()))

        val message =
            VarseldefinisjonMessage(
                id = packet["gjeldende_definisjon.id"].asUUID(),
                varselkode = packet["varselkode"].asText(),
                tittel = packet["gjeldende_definisjon.tittel"].asText(),
                forklaring = packet["gjeldende_definisjon.forklaring"].takeUnless(JsonNode::isMissingOrNull)?.textValue(),
                handling = packet["gjeldende_definisjon.handling"].takeUnless(JsonNode::isMissingOrNull)?.textValue(),
                avviklet = packet["gjeldende_definisjon.avviklet"].asBoolean(),
                opprettet = packet["gjeldende_definisjon.opprettet"].asLocalDateTime(),
            )
        message.sendInnTil(mediator)
    }
}

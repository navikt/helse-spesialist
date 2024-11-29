package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.VarseldefinisjonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VarseldefinisjonRiver(
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

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerlogg.error("Forstod ikke varselkode_ny_definisjon:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        sikkerlogg.info("Mottok melding om ny definisjon for {}", kv("varselkode", packet["varselkode"].asText()))

        val message = VarseldefinisjonMessage(packet)
        message.sendInnTil(mediator)
    }
}

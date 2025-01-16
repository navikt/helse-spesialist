package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import org.slf4j.LoggerFactory

class MidnattRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireAny("@event_name", listOf("midnatt", "slett_gamle_dokumenter_spesialist"))
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["@id"].asUUID()
        logg.info("Mottok melding midnatt, {}", kv("hendelseId", hendelseId))

        val antallSlettet = mediator.slettGamleDokumenter()
        logg.info("Slettet $antallSlettet dokumenter")
    }
}

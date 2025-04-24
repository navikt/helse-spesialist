package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.registrerTidsbrukForBehov
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.toKotlinDuration

class BehovtidsbrukMetrikkRiver : SpesialistRiver {
    private val logg: Logger = LoggerFactory.getLogger("MetrikkRiver")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireKey("@løsning")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@besvart", "@behov", "system_participating_services")
            it.interestedIn("@løsning.Godkjenning.godkjent")
            it.interestedIn("@løsning.Godkjenning.automatiskBehandling")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val besvart = packet["@besvart"].asLocalDateTime()
        val opprettet = packet["system_participating_services"][0].let { it["time"].asLocalDateTime() }
        val delay = Duration.between(opprettet, besvart).toKotlinDuration()
        val behov = packet["@behov"].map(JsonNode::asText)
        val godkjent: Boolean? = packet["@løsning.Godkjenning.godkjent"].takeUnless { it.isMissingOrNull() }?.asBoolean()
        val automatisk: Boolean? = packet["@løsning.Godkjenning.automatiskBehandling"].takeUnless { it.isMissingOrNull() }?.asBoolean()

        val godkjenningslog =
            if (godkjent != null && automatisk != null) {
                " Løsning er ${if (automatisk) "automatisk" else "manuelt"} ${if (godkjent) "godkjent" else "avvist"}."
            } else {
                ""
            }

        logg.info("Registrerer svartid for $behov som $delay.$godkjenningslog")
        registrerTidsbrukForBehov(behov.first(), delay)
    }
}

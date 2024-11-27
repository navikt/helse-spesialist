package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.registrerTidsbrukForBehov
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

internal class MetrikkRiver : SpesialistRiver {
    val log: Logger = LoggerFactory.getLogger("MetrikkRiver")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.requireKey("@besvart", "@behov", "system_participating_services")
            it.interestedIn("@løsning.Godkjenning.godkjent")
            it.interestedIn("@løsning.Godkjenning.automatiskBehandling")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val besvart = packet["@besvart"].asLocalDateTime()
        val opprettet = packet["system_participating_services"][0].let { it["time"].asLocalDateTime() }
        val delay = ChronoUnit.MILLIS.between(opprettet, besvart)
        val behov = packet["@behov"].map(JsonNode::asText)
        val godkjent: Boolean? = packet["@løsning.Godkjenning.godkjent"].takeUnless { it.isMissingOrNull() }?.asBoolean()
        val automatisk: Boolean? = packet["@løsning.Godkjenning.automatiskBehandling"].takeUnless { it.isMissingOrNull() }?.asBoolean()

        val godkjenningslog =
            if (godkjent != null && automatisk != null) {
                " Løsning er ${if (automatisk) "automatisk" else "manuelt"} ${if (godkjent) "godkjent" else "avvist"}."
            } else {
                ""
            }

        log.info("Registrerer svartid for $behov som $delay ms.$godkjenningslog")
        registrerTidsbrukForBehov.labels(behov.first()).observe(delay.toDouble())
    }
}

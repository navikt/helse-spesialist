package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asInstant
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import java.util.UUID

class SisRiver : SpesialistRiver {
    override fun preconditions() =
        River.PacketValidation {
            it.forbid("@event_name")
            it.requireValue("status", "OPPRETTET")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("vedtaksperiodeId", "behandlingId", "tidspunkt", "status")
            it.requireArray("eksterneSøknadIder")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().let(UUID::fromString)
        val behandlingId = packet["behandlingId"].asText().let(UUID::fromString)
        val tidspunkt = packet["tidspunkt"].asInstant()
        val eksterneSøknadIder = packet["eksterneSøknadIder"].map { it.asText().let(UUID::fromString) }.toSet()

        logg.info("Mottok opprettet melding på sis topic, vedtaksperiode: $vedtaksperiodeId, beandlingId: $behandlingId")
    }
}

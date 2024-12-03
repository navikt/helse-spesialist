package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.overstyring.OverstyringIgangsatt

internal class OverstyringIgangsattRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "overstyring_igangsatt")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("kilde")
            it.requireArray("berørtePerioder") {
                requireKey("vedtaksperiodeId")
            }
            it.requireKey("@id")
            it.requireKey("fødselsnummer")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(OverstyringIgangsatt(packet), context)
    }
}

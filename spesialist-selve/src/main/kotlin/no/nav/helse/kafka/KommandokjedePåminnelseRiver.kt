package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.påminnelser.KommandokjedePåminnelse

internal class KommandokjedePåminnelseRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "kommandokjede_påminnelse")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "commandContextId", "meldingId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldingId = packet["meldingId"].asUUID()
        val kommandokjede =
            KommandokjedePåminnelse(
                commandContextId = packet["commandContextId"].asUUID(),
                meldingId = packet["meldingId"].asUUID(),
            )

        mediator.påminnelse(
            meldingId,
            kommandokjede.commandContextId,
            kommandokjede.meldingId,
            kommandokjede,
            context,
        )
    }
}

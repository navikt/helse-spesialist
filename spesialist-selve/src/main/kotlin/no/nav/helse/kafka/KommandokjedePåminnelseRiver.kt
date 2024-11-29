package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.påminnelser.KommandokjedePåminnelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class KommandokjedePåminnelseRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "kommandokjede_påminnelse")
            it.requireKey("@id", "commandContextId", "meldingId")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.error("Forstod ikke kommandokjede_påminnelse:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldingId = packet["meldingId"].asUUID()
        logg.info(
            "Mottok kommandokjede_påminnelse med {}",
            StructuredArguments.keyValue("meldingId", meldingId),
        )
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

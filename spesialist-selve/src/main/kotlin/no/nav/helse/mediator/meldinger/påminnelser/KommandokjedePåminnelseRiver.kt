package no.nav.helse.mediator.meldinger.påminnelser

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

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
    ) {
        logg.error("Forstod ikke kommandokjede_påminnelse:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val meldingId = UUID.fromString(packet["@id"].asText())
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

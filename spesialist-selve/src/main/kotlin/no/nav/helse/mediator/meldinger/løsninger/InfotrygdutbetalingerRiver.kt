package no.nav.helse.mediator.meldinger.løsninger

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class InfotrygdutbetalingerRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("HentInfotrygdutbetalinger"))
            it.requireKey("@id", "contextId", "hendelseId")
            it.requireKey("@løsning.HentInfotrygdutbetalinger")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("forstod ikke HentInfotrygdutbetalinger:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        sikkerlogg.info(
            "Mottok HentInfotrygdutbetalinger for {}, {}",
            kv("hendelseId", hendelseId),
            kv("contextId", contextId),
        )
        mediator.løsning(
            hendelseId,
            contextId,
            UUID.fromString(packet["@id"].asText()),
            HentInfotrygdutbetalingerløsning(packet["@løsning.HentInfotrygdutbetalinger"]),
            context,
        )
    }
}

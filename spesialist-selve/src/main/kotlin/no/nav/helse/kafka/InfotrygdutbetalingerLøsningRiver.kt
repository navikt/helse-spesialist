package no.nav.helse.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class InfotrygdutbetalingerLøsningRiver(
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
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        sikkerlogg.info(
            "Mottok HentInfotrygdutbetalinger for {}, {}",
            kv("hendelseId", hendelseId),
            kv("contextId", contextId),
        )
        mediator.løsning(
            hendelseId,
            contextId,
            packet["@id"].asUUID(),
            HentInfotrygdutbetalingerløsning(packet["@løsning.HentInfotrygdutbetalinger"]),
            context,
        )
    }
}

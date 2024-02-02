package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.LoggerFactory

internal class SaksbehandlerløsningRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
) : River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "saksbehandler_løsning")
                    it.requireKey("@id", "fødselsnummer", "oppgaveId", "hendelseId", "behandlingId")
                    it.requireKey("godkjent", "saksbehandlerident", "saksbehandleroid", "saksbehandlerepost")
                    it.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
                    it.interestedIn("årsak", "begrunnelser", "kommentar", "saksbehandleroverstyringer")
                }
            }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("forstod ikke saksbehandlerløsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val id = UUID.fromString(packet["@id"].asText())
        if (id == UUID.fromString("d622983f-c054-4f97-97a1-b941d0c8ea23")) return
        mediator.saksbehandlerløsning(
            message = packet,
            id = id,
            behandlingId = UUID.fromString(packet["behandlingId"].asText()),
            godkjenningsbehovhendelseId = hendelseId,
            fødselsnummer = packet["fødselsnummer"].asText(),
            godkjent = packet["godkjent"].asBoolean(),
            saksbehandlerident = packet["saksbehandlerident"].asText(),
            saksbehandlerepost = packet["saksbehandlerepost"].asText(),
            godkjenttidspunkt = packet["godkjenttidspunkt"].asLocalDateTime(),
            årsak = packet["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
            begrunnelser = packet["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
            kommentar = packet["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
            saksbehandleroverstyringer = packet["saksbehandleroverstyringer"].takeUnless(JsonNode::isMissingOrNull)?.map {
                UUID.fromString(it.asText())
            } ?: emptyList(),
            oppgaveId = packet["oppgaveId"].asLong(),
            context = context
        )
    }
}
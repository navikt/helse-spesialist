package no.nav.helse.modell.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerLøsning(
    private val godkjent: Boolean,
    private val saksbehandlerIdent: String,
    private val oid: UUID,
    private val epostadresse: String,
    private val godkjenttidspunkt: LocalDateTime,
    private val årsak: String?,
    private val begrunnelser: List<String>?,
    private val kommentar: String?,
    private val oppgaveId: Long
) {
    fun ferdigstillOppgave(oppgave: Oppgave, behov: UtbetalingsgodkjenningMessage) {
        oppgave.ferdigstill(oppgaveId, saksbehandlerIdent, oid)
        behov.løs(godkjent, saksbehandlerIdent, godkjenttidspunkt, årsak, begrunnelser, kommentar)
    }

    internal class SaksbehandlerLøsningRiver(rapidsConnection: RapidsConnection, private val mediator: IHendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "saksbehandler_løsning")
                        it.requireKey("oppgaveId", "contextId", "hendelseId")
                        it.requireKey("godkjent", "saksbehandlerident", "saksbehandleroid", "saksbehandlerepost")
                        it.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
                        it.interestedIn("årsak", "begrunnelser", "kommentar")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke saksbehandlerløsning:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, SaksbehandlerLøsning(
                packet["godkjent"].asBoolean(),
                packet["saksbehandlerident"].asText(),
                UUID.fromString(packet["saksbehandleroid"].asText()),
                packet["saksbehandlerepost"].asText(),
                packet["godkjenttidspunkt"].asLocalDateTime(),
                packet["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                packet["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
                packet["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                packet["oppgaveId"].asLong()
            ), context)
        }
    }
}

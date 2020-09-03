package no.nav.helse.modell.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.Oppgavestatus
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerLøsning(
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val oid: UUID,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?
) {
    fun ferdigstillOppgave(oppgaveDao: OppgaveDao, eventId: UUID, oppgavetype: String, løsning: JsonMessage) {
        oppgaveDao.updateOppgave(
            eventId = eventId,
            oppgavetype = oppgavetype,
            oppgavestatus = Oppgavestatus.Ferdigstilt,
            ferdigstiltAv = epostadresse,
            oid = oid
        )
        løsning["@løsning"] = mapOf(
            "Godkjenning" to mapOf(
                "godkjent" to godkjent,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "årsak" to årsak,
                "begrunnelser" to begrunnelser,
                "kommentar" to kommentar
            ))
    }

    internal class SaksbehandlerLøsningRiver(rapidsConnection: RapidsConnection, private val mediator: IHendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "saksbehandler_løsning")
                        it.requireKey("spleisBehovId", "contextId")
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
            val hendelseId = UUID.fromString(packet["spleisBehovId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, SaksbehandlerLøsning(
                packet["godkjent"].asBoolean(),
                packet["saksbehandlerident"].asText(),
                UUID.fromString(packet["saksbehandleroid"].asText()),
                packet["saksbehandlerepost"].asText(),
                packet["godkjenttidspunkt"].asLocalDateTime(),
                packet["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                packet["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
                packet["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText()
            ), context)
        }
    }
}

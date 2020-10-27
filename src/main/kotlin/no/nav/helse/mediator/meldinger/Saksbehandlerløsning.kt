package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.OppgaveDao
import no.nav.helse.modell.kommando.FerdigstillOppgaveCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.UtbetalingsgodkjenningCommand
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class Saksbehandlerløsning(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    godkjent: Boolean,
    saksbehandlerIdent: String,
    oid: UUID,
    epostadresse: String,
    godkjenttidspunkt: LocalDateTime,
    årsak: String?,
    begrunnelser: List<String>?,
    kommentar: String?,
    private val oppgaveId: Long,
    godkjenningsbehovhendelseId: UUID,
    hendelseDao: HendelseDao,
    oppgaveMediator: OppgaveMediator,
    private val oppgaveDao: OppgaveDao,
    godkjenningMediator: GodkjenningMediator
) : Hendelse, MacroCommand() {

    override val commands = listOf(
        UtbetalingsgodkjenningCommand(godkjent, saksbehandlerIdent, oid, epostadresse, godkjenttidspunkt, årsak, begrunnelser, kommentar, godkjenningsbehovhendelseId, hendelseDao, godkjenningMediator, vedtaksperiodeId(), fødselsnummer),
        FerdigstillOppgaveCommand(oppgaveMediator, saksbehandlerIdent, oid, oppgaveId, oppgaveDao)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
    override fun toJson() = json

    internal class SaksbehandlerløsningRiver(rapidsConnection: RapidsConnection, private val mediator: IHendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "saksbehandler_løsning")
                        it.requireKey("@id", "fødselsnummer", "oppgaveId", "contextId", "hendelseId")
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
            mediator.saksbehandlerløsning(
                packet,
                UUID.fromString(packet["@id"].asText()),
                hendelseId,
                contextId,
                packet["fødselsnummer"].asText(),
                packet["godkjent"].asBoolean(),
                packet["saksbehandlerident"].asText(),
                UUID.fromString(packet["saksbehandleroid"].asText()),
                packet["saksbehandlerepost"].asText(),
                packet["godkjenttidspunkt"].asLocalDateTime(),
                packet["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                packet["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
                packet["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                packet["oppgaveId"].asLong(),
                context
            )
        }
    }
}

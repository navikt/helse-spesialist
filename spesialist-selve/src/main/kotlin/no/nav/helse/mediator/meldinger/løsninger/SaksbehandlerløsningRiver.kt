package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class SaksbehandlerløsningRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                validate { message ->
                    message.demandValue("@event_name", "saksbehandler_løsning")
                    message.requireKey("@id", "fødselsnummer", "oppgaveId", "hendelseId", "behandlingId")
                    message.requireKey("godkjent", "saksbehandlerident", "saksbehandleroid", "saksbehandlerepost")
                    message.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
                    message.requireKey("saksbehandler", "saksbehandler.ident", "saksbehandler.epostadresse")
                    message.interestedIn("beslutter", "beslutter.ident", "beslutter.epostadresse")
                    message.interestedIn("årsak", "begrunnelser", "kommentar", "saksbehandleroverstyringer")
                }
            }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("forstod ikke saksbehandlerløsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (UUID.fromString(packet["@id"].asText()) == UUID.fromString("7af5fede-749d-4718-ac91-444996600236")) return
        val fødselsnummer = packet["fødselsnummer"].asText()
        mediator.håndter(fødselsnummer, Saksbehandlerløsning(packet), context)
    }
}
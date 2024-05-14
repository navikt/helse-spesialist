package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.util.UUID

internal class SaksbehandlerløsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "saksbehandler_løsning")
            it.requireKey("@id", "fødselsnummer", "oppgaveId", "hendelseId", "behandlingId")
            it.requireKey("godkjent", "saksbehandlerident", "saksbehandleroid", "saksbehandlerepost")
            it.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
            it.requireKey("saksbehandler", "saksbehandler.ident", "saksbehandler.epostadresse")
            it.interestedIn("beslutter", "beslutter.ident", "beslutter.epostadresse")
            it.interestedIn("årsak", "begrunnelser", "kommentar", "saksbehandleroverstyringer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLog.error("forstod ikke saksbehandlerløsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        if (UUID.fromString(packet["@id"].asText()) == UUID.fromString("7af5fede-749d-4718-ac91-444996600236")) return
        val fødselsnummer = packet["fødselsnummer"].asText()
        mediator.håndter(fødselsnummer, Saksbehandlerløsning(packet), context)
    }
}

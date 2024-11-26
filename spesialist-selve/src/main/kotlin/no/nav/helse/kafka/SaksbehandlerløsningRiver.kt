package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import org.slf4j.LoggerFactory

internal class SaksbehandlerløsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "saksbehandler_løsning")
            it.requireKey("@id", "fødselsnummer", "oppgaveId", "hendelseId")
            it.requireKey("godkjent", "saksbehandlerident", "saksbehandleroid", "saksbehandlerepost")
            it.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
            it.requireKey("saksbehandler", "saksbehandler.ident", "saksbehandler.epostadresse")
            it.interestedIn("beslutter", "beslutter.ident", "beslutter.epostadresse")
            it.interestedIn("årsak", "begrunnelser", "kommentar", "saksbehandleroverstyringer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLog.error("forstod ikke saksbehandlerløsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(Saksbehandlerløsning(packet), context)
    }
}

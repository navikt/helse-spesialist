package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning

internal class SaksbehandlerløsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "saksbehandler_løsning")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "oppgaveId", "hendelseId")
            it.requireKey("godkjent", "saksbehandlerident", "saksbehandleroid", "saksbehandlerepost")
            it.require("godkjenttidspunkt", JsonNode::asLocalDateTime)
            it.requireKey("saksbehandler", "saksbehandler.ident", "saksbehandler.epostadresse")
            it.interestedIn("beslutter", "beslutter.ident", "beslutter.epostadresse")
            it.interestedIn("årsak", "begrunnelser", "kommentar", "saksbehandleroverstyringer")
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

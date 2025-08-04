package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning.Saksbehandler
import java.util.UUID

class SaksbehandlerløsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "saksbehandler_løsning")
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
        mediator.mottaMelding(
            Saksbehandlerløsning(
                id = packet["@id"].asUUID(),
                oppgaveId = packet["oppgaveId"].asLong(),
                godkjenningsbehovhendelseId = packet["hendelseId"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                godkjent = packet["godkjent"].asBoolean(),
                ident = packet["saksbehandlerident"].asText(),
                epostadresse = packet["saksbehandlerepost"].asText(),
                godkjenttidspunkt = packet["godkjenttidspunkt"].asLocalDateTime(),
                årsak = packet["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                begrunnelser = packet["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
                kommentar = packet["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                saksbehandler =
                    Saksbehandler(
                        packet["saksbehandler.ident"].asText(),
                        packet["saksbehandler.epostadresse"].asText(),
                    ),
                beslutter =
                    packet["beslutter"].takeUnless(JsonNode::isMissingOrNull)?.let {
                        Saksbehandler(
                            packet["beslutter.ident"].asText(),
                            packet["beslutter.epostadresse"].asText(),
                        )
                    },
                saksbehandleroverstyringer =
                    packet["saksbehandleroverstyringer"].takeUnless(JsonNode::isMissingOrNull)?.map {
                        UUID.fromString(it.asText())
                    } ?: emptyList(),
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }
}

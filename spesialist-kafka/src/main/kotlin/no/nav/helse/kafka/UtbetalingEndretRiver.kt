package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values
import tools.jackson.databind.JsonNode

class UtbetalingEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "utbetaling_endret")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "utbetalingId", "type")
            it.interestedIn(
                "arbeidsgiverOppdrag.nettoBeløp",
                "personOppdrag.nettoBeløp",
            )
            it.requireKey("arbeidsgiverOppdrag", "personOppdrag")
            it.requireAny("forrigeStatus", Utbetalingsstatus.gyldigeStatuser.values())
            it.requireAny("gjeldendeStatus", Utbetalingsstatus.gyldigeStatuser.values())
            it.require("@opprettet", JsonNode::asLocalDateTime)
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(
            UtbetalingEndret(
                id = packet["@id"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asString(),
                organisasjonsnummer = packet["organisasjonsnummer"].asString(),
                utbetalingId = packet["utbetalingId"].asUUID(),
                type = packet["type"].asString(),
                gjeldendeStatus = Utbetalingsstatus.valueOf(packet["gjeldendeStatus"].asString()),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                arbeidsgiverbeløp = packet["arbeidsgiverOppdrag"]["nettoBeløp"].asInt(),
                personbeløp = packet["personOppdrag"]["nettoBeløp"].asInt(),
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }
}

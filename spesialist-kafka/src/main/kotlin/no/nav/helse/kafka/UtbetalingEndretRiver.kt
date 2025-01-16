package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values

class UtbetalingEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "utbetaling_endret")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "organisasjonsnummer", "utbetalingId", "type")
            // disse brukes i Hendelsefabrikk for å lagre oppdrag i db
            it.requireKey("arbeidsgiverOppdrag.fagsystemId", "personOppdrag.fagsystemId")
            it.interestedIn(
                "arbeidsgiverOppdrag.mottaker",
                "personOppdrag.mottaker",
                "arbeidsgiverOppdrag.nettoBeløp",
                "personOppdrag.nettoBeløp",
            )
            it.requireKey("arbeidsgiverOppdrag", "personOppdrag")
            it.requireArray("arbeidsgiverOppdrag.linjer") {
                require("fom", JsonNode::asLocalDate)
                require("tom", JsonNode::asLocalDate)
                interestedIn("totalbeløp")
            }
            it.requireArray("personOppdrag.linjer") {
                require("fom", JsonNode::asLocalDate)
                require("tom", JsonNode::asLocalDate)
                interestedIn("totalbeløp")
            }
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
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                utbetalingId = packet["utbetalingId"].asUUID(),
                type = packet["type"].asText(),
                gjeldendeStatus = Utbetalingsstatus.valueOf(packet["gjeldendeStatus"].asText()),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                arbeidsgiverbeløp = packet["arbeidsgiverOppdrag"]["nettoBeløp"].asInt(),
                personbeløp = packet["personOppdrag"]["nettoBeløp"].asInt(),
                arbeidsgiverOppdrag =
                    tilOppdrag(
                        packet["arbeidsgiverOppdrag"],
                        packet["organisasjonsnummer"].asText(),
                    ),
                personOppdrag = tilOppdrag(packet["personOppdrag"], packet["fødselsnummer"].asText()),
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }

    private companion object {
        private fun tilOppdrag(
            jsonNode: JsonNode,
            mottaker: String,
        ) = LagreOppdragCommand.Oppdrag(
            fagsystemId = jsonNode.path("fagsystemId").asText(),
            mottaker = jsonNode.path("mottaker").takeIf(JsonNode::isTextual)?.asText() ?: mottaker,
        )
    }
}

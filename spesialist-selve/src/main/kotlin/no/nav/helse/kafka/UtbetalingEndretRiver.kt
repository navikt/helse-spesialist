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
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values

internal class UtbetalingEndretRiver(
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
        mediator.mottaMelding(UtbetalingEndret(packet), context)
    }
}

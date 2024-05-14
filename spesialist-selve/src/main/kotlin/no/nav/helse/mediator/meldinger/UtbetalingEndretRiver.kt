package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class UtbetalingEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "utbetaling_endret")
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

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke utbetaling_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        if (ignorer(packet)) return
        val fødselsnummer = packet["fødselsnummer"].asText()
        val gjeldendeStatus = packet["gjeldendeStatus"].asText()

        sikkerLogg.info(
            "Mottok utbetaling_endret for {}, {} med status {}",
            keyValue("fødselsnummer", fødselsnummer),
            keyValue("utbetalingId", utbetalingId),
            keyValue("gjeldendeStatus", gjeldendeStatus),
        )
        mediator.mottaMelding(UtbetalingEndret(packet), context)
    }

    private fun ignorer(packet: JsonMessage) =
        packet["arbeidsgiverOppdrag.fagsystemId"].asText() in
            setOf(
                "2YG7TGWMJJHNPPY5JP5CHUIFII",
                "PJV6BYWUEVFCZIDNXG4MYR6ICM",
                "GUEFZE3CIJHHJNYIIOWG3XJUW4",
                "FT574IJ4AZCXRHXNMSEWJV7JBM",
            )
}

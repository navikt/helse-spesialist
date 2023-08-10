package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class UtbetalingEndretRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "utbetaling_endret")
                it.requireKey(
                    "@id", "fødselsnummer", "organisasjonsnummer",
                    "utbetalingId")
                // disse brukes i Hendelsefabrikk for å lagre oppdrag i db
                it.requireKey("arbeidsgiverOppdrag.fagsystemId", "personOppdrag.fagsystemId")
                it.interestedIn("arbeidsgiverOppdrag.mottaker", "personOppdrag.mottaker",
                    "arbeidsgiverOppdrag.nettoBeløp", "personOppdrag.nettoBeløp")
                /*
                it.requireArray("arbeidsgiverOppdrag.linjer") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    interestedIn("totalbeløp")
                }
                it.requireArray("personOppdrag.linjer") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                    interestedIn("totalbeløp")
                }*/
                it.requireAny("forrigeStatus", Utbetalingsstatus.gyldigeStatuser.values())
                it.requireAny("gjeldendeStatus", Utbetalingsstatus.gyldigeStatuser.values())
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Forstod ikke utbetaling_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val fødselsnummer = packet["fødselsnummer"].asText()
        val orgnummer = packet["organisasjonsnummer"].asText()
        val gjeldendeStatus = packet["gjeldendeStatus"].asText()

        sikkerLogg.info(
            "Mottok utbetaling_endret for {}, {} med status {}",
            StructuredArguments.keyValue("fødselsnummer", fødselsnummer),
            StructuredArguments.keyValue("utbetalingId", utbetalingId),
            StructuredArguments.keyValue("gjeldendeStatus", gjeldendeStatus)
        )
        mediator.utbetalingEndret(fødselsnummer, orgnummer, packet, context)
    }
}
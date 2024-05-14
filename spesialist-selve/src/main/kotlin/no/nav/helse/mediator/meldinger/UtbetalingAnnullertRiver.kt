package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class UtbetalingAnnullertRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val log = LoggerFactory.getLogger("UtbetalingAnnullert")
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "utbetaling_annullert")
            it.requireKey(
                "@id",
                "fødselsnummer",
                "utbetalingId",
                "tidspunkt",
                "epost",
            )
            it.interestedIn("arbeidsgiverFagsystemId", "personFagsystemId")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke utbetaling_annullert:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val id = UUID.fromString(packet["@id"].asText())
        val arbeidsgiverFagsystemId = packet["arbeidsgiverFagsystemId"].takeUnless { it.isMissingOrNull() }?.asText()
        val personFagsystemId = packet["personFagsystemId"].takeUnless { it.isMissingOrNull() }?.asText()

        val logInfo =
            mutableListOf(kv("eventId", id)).also {
                if (arbeidsgiverFagsystemId != null) {
                    it.add(kv("arbeidsgiverFagsystemId", arbeidsgiverFagsystemId))
                }
                if (personFagsystemId != null) {
                    it.add(kv("personFagsystemId", personFagsystemId))
                }
            }

        log.info(
            "Mottok utbetaling_annullert ${logInfo.joinToString(transform = { "{}" })}",
            *logInfo.toTypedArray(),
        )
        mediator.mottaMelding(UtbetalingAnnullert(packet), context)
    }
}

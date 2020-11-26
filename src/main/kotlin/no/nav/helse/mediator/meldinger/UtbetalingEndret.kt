package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val utbetalingId: UUID,
    private val type: String,
    private val status: String,
    private val opprettet: LocalDateTime,
    private val arbeidsgiverFagsystemId: String,
    private val personFagsystemId: String,
    private val json: String,
    private val utbetalingDao: UtbetalingDao
) : Hendelse {
    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingEndret::class.java)
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    override fun execute(context: CommandContext): Boolean {
        log.info("lagrer utbetaling $utbetalingId med status $status")
        utbetalingDao.lagre(
            utbetalingId,
            fødselsnummer,
            orgnummer,
            type,
            status,
            opprettet,
            arbeidsgiverFagsystemId,
            personFagsystemId,
            json
        )
        return true
    }

    override fun resume(context: CommandContext) = true

    override fun undo(context: CommandContext) {}

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private val godkjenteStatuser = listOf("GODKJENT", "SENDT", "OVERFØRT", "UTBETALING_FEILET", "UTBETALT", "ANNULLERT")
        private val gyldigeStatuser = listOf("IKKE_UTBETALT", "IKKE_GODKJENT", "GODKJENT_UTEN_UTBETALING") + godkjenteStatuser

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "utbetaling_endret")
                    it.requireKey("@id", "fødselsnummer", "organisasjonsnummer",
                        "utbetalingId", "arbeidsgiverOppdrag.fagsystemId", "personOppdrag.fagsystemId"
                    )
                    it.requireAny("type", listOf("UTBETALING", "ANNULLERING", "ETTERUTBETALING"))
                    it.requireAny("gjeldendeStatus", gyldigeStatuser)
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke utbetaling_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val status = packet["gjeldendeStatus"].asText()
            if (status !in godkjenteStatuser) return

            val id = UUID.fromString(packet["utbetalingId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val orgnummer = packet["organisasjonsnummer"].asText()
            sikkerLogg.info(
                "Mottok utbetalt event for {}, {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("utbetalingId", id)
            )
            mediator.utbetalingEndret(fødselsnummer, orgnummer, packet, context)
        }
    }
}

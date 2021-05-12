package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.opptegnelse.OpptegnelseDao
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.gyldigeStatuser
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    orgnummer: String,
    utbetalingId: UUID,
    type: String,
    gjeldendeStatus: Utbetalingsstatus,
    forrigeStatus: Utbetalingsstatus,
    opprettet: LocalDateTime,
    arbeidsgiverOppdrag: LagreOppdragCommand.Oppdrag,
    personOppdrag: LagreOppdragCommand.Oppdrag,
    private val json: String,
    utbetalingDao: UtbetalingDao,
    opptegnelseDao: OpptegnelseDao,
    oppgaveDao: OppgaveDao,
    oppgaveMediator: OppgaveMediator
) : Hendelse, MacroCommand() {

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json
    override val commands: List<Command> = listOf(
        LagreOppdragCommand(
            fødselsnummer,
            orgnummer,
            utbetalingId,
            type,
            gjeldendeStatus,
            forrigeStatus,
            opprettet,
            arbeidsgiverOppdrag,
            personOppdrag,
            json,
            utbetalingDao,
            opptegnelseDao
        ),
        OppdaterOppgavestatusCommand(utbetalingId, gjeldendeStatus, oppgaveDao, oppgaveMediator)
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "utbetaling_endret")
                    it.requireKey(
                        "@id", "fødselsnummer", "organisasjonsnummer",
                        "utbetalingId", "arbeidsgiverOppdrag.fagsystemId", "personOppdrag.fagsystemId"
                    )
                    it.requireAny("type", listOf("UTBETALING", "ANNULLERING", "ETTERUTBETALING", "FERIEPENGER"))
                    it.requireAny("forrigeStatus", gyldigeStatuser.values())
                    it.requireAny("gjeldendeStatus", gyldigeStatuser.values())
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke utbetaling_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
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

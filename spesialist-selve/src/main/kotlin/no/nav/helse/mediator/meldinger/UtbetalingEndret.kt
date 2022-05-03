package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.abonnement.OpptegnelseDao
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.gyldigeStatuser
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
            Utbetalingtype.valueOf(type),
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
                    it.requireAny("type", Utbetalingtype.gyldigeTyper.values())
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
            val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val orgnummer = packet["organisasjonsnummer"].asText()
            val utbetalingType : Utbetalingtype = Utbetalingtype.valueOf(packet["type"].asText())

            sikkerLogg.info(
                "Mottok utbetaling_endret for {}, {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("utbetalingId", utbetalingId)
            )
            mediator.utbetalingEndret(fødselsnummer, orgnummer, utbetalingId, utbetalingType, packet, context)
        }
    }
}

package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.gyldigeStatuser
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.Companion.values
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.InvaliderUtbetalingForGenerasjonerCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class UtbetalingEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    orgnummer: String,
    utbetalingId: UUID,
    type: String,
    gjeldendeStatus: Utbetalingsstatus,
    opprettet: LocalDateTime,
    arbeidsgiverbeløp: Int,
    personbeløp: Int,
    arbeidsgiverOppdrag: LagreOppdragCommand.Oppdrag,
    personOppdrag: LagreOppdragCommand.Oppdrag,
    private val json: String,
    utbetalingDao: UtbetalingDao,
    opptegnelseDao: OpptegnelseDao,
    oppgaveDao: OppgaveDao,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    oppgaveMediator: OppgaveMediator,
    gjeldendeGenerasjoner: List<Generasjon>
) : Hendelse, MacroCommand() {

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json
    override val commands: List<Command> = mutableListOf(
        LagreOppdragCommand(
            fødselsnummer = fødselsnummer,
            orgnummer = orgnummer,
            utbetalingId = utbetalingId,
            type = Utbetalingtype.valueOf(type),
            status = gjeldendeStatus,
            opprettet = opprettet,
            arbeidsgiverOppdrag = arbeidsgiverOppdrag,
            personOppdrag = personOppdrag,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            json = json,
            utbetalingDao = utbetalingDao,
            opptegnelseDao = opptegnelseDao
        ),
        ReserverPersonHvisTildeltCommand(
            fødselsnummer = fødselsnummer,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            oppgaveDao = oppgaveDao
        ),
        OppdaterOppgavestatusCommand(utbetalingId, gjeldendeStatus, oppgaveDao, oppgaveMediator),
    ).apply {
        if (gjeldendeStatus == FORKASTET)
            add(InvaliderUtbetalingForGenerasjonerCommand(utbetalingId, gjeldendeGenerasjoner))
    }

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "utbetaling_endret")
                    it.requireKey(
                        "@id", "fødselsnummer", "organisasjonsnummer",
                        "utbetalingId", "arbeidsgiverOppdrag.fagsystemId", "personOppdrag.fagsystemId",
                        "arbeidsgiverOppdrag.nettoBeløp", "personOppdrag.nettoBeløp"
                    )
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
            val gjeldendeStatus = packet["gjeldendeStatus"].asText()

            sikkerLogg.info(
                "Mottok utbetaling_endret for {}, {} med status {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("utbetalingId", utbetalingId),
                keyValue("gjeldendeStatus", gjeldendeStatus)
            )
            mediator.utbetalingEndret(fødselsnummer, orgnummer, packet, context)
        }
    }
}

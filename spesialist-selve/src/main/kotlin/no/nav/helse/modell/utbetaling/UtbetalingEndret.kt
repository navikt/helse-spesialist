package no.nav.helse.modell.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingEndret private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val organisasjonsnummer: String,
    val utbetalingId: UUID,
    val type: String,
    val gjeldendeStatus: Utbetalingsstatus,
    val opprettet: LocalDateTime,
    val arbeidsgiverbeløp: Int,
    val personbeløp: Int,
    val arbeidsgiverOppdrag: LagreOppdragCommand.Oppdrag,
    val personOppdrag: LagreOppdragCommand.Oppdrag,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
        type = packet["type"].asText(),
        gjeldendeStatus = Utbetalingsstatus.valueOf(packet["gjeldendeStatus"].asText()),
        opprettet = packet["@opprettet"].asLocalDateTime(),
        arbeidsgiverbeløp = packet["arbeidsgiverOppdrag"]["nettoBeløp"].asInt(),
        personbeløp = packet["personOppdrag"]["nettoBeløp"].asInt(),
        arbeidsgiverOppdrag = tilOppdrag(packet["arbeidsgiverOppdrag"], packet["organisasjonsnummer"].asText()),
        personOppdrag = tilOppdrag(packet["personOppdrag"], packet["fødselsnummer"].asText()),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
        type = jsonNode["type"].asText(),
        gjeldendeStatus = Utbetalingsstatus.valueOf(jsonNode["gjeldendeStatus"].asText()),
        opprettet = jsonNode["@opprettet"].asLocalDateTime(),
        arbeidsgiverbeløp = jsonNode["arbeidsgiverOppdrag"]["nettoBeløp"].asInt(),
        personbeløp = jsonNode["personOppdrag"]["nettoBeløp"].asInt(),
        arbeidsgiverOppdrag = tilOppdrag(jsonNode["arbeidsgiverOppdrag"], jsonNode["organisasjonsnummer"].asText()),
        personOppdrag = tilOppdrag(jsonNode["personOppdrag"], jsonNode["fødselsnummer"].asText()),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandofabrikk: Kommandofabrikk,
    ) {
        if (gjeldendeStatus == FORKASTET) person.utbetalingForkastet(this)
        kommandofabrikk.iverksettUtbetalingEndret(this)
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json

    fun erRelevantFor(gjeldendeUtbetalingId: UUID) = utbetalingId == gjeldendeUtbetalingId

    private companion object {
        private fun tilOppdrag(
            jsonNode: JsonNode,
            mottaker: String,
        ) = LagreOppdragCommand.Oppdrag(
            fagsystemId = jsonNode.path("fagsystemId").asText(),
            mottaker = jsonNode.path("mottaker").takeIf(JsonNode::isTextual)?.asText() ?: mottaker,
            linjer =
                jsonNode.path("linjer").map { linje ->
                    LagreOppdragCommand.Oppdrag.Utbetalingslinje(
                        fom = linje.path("fom").asLocalDate(),
                        tom = linje.path("tom").asLocalDate(),
                        totalbeløp = linje.path("totalbeløp").takeIf(JsonNode::isInt)?.asInt(),
                    )
                },
        )
    }
}

internal class UtbetalingEndretCommand(
    fødselsnummer: String,
    organisasjonsnummer: String,
    utbetalingId: UUID,
    utbetalingstype: String,
    gjeldendeStatus: Utbetalingsstatus,
    opprettet: LocalDateTime,
    arbeidsgiverOppdrag: LagreOppdragCommand.Oppdrag,
    personOppdrag: LagreOppdragCommand.Oppdrag,
    arbeidsgiverbeløp: Int,
    personbeløp: Int,
    utbetalingDao: UtbetalingDao,
    opptegnelseDao: OpptegnelseDao,
    reservasjonDao: ReservasjonDao,
    oppgaveDao: OppgaveDao,
    tildelingDao: TildelingDao,
    oppgaveMediator: OppgaveMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    json: String,
) : MacroCommand() {
    override val commands: List<Command> =
        mutableListOf(
            LagreOppdragCommand(
                fødselsnummer = fødselsnummer,
                orgnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                type = Utbetalingtype.valueOf(utbetalingstype),
                status = gjeldendeStatus,
                opprettet = opprettet,
                arbeidsgiverOppdrag = arbeidsgiverOppdrag,
                personOppdrag = personOppdrag,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                json = json,
                utbetalingDao = utbetalingDao,
                opptegnelseDao = opptegnelseDao,
            ),
            ReserverPersonHvisTildeltCommand(
                fødselsnummer = fødselsnummer,
                reservasjonDao = reservasjonDao,
                tildelingDao = tildelingDao,
                oppgaveDao = oppgaveDao,
                totrinnsvurderingMediator = totrinnsvurderingMediator,
            ),
            OppdaterOppgavestatusCommand(utbetalingId, gjeldendeStatus, oppgaveMediator),
        )
}

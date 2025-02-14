package no.nav.helse.modell.utbetaling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingEndret(
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
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
        type = jsonNode["type"].asText(),
        gjeldendeStatus = Utbetalingsstatus.valueOf(jsonNode["gjeldendeStatus"].asText()),
        opprettet = jsonNode["@opprettet"].asText().let(LocalDateTime::parse),
        arbeidsgiverbeløp = jsonNode["arbeidsgiverOppdrag"]["nettoBeløp"].asInt(),
        personbeløp = jsonNode["personOppdrag"]["nettoBeløp"].asInt(),
        arbeidsgiverOppdrag = tilOppdrag(jsonNode["arbeidsgiverOppdrag"], jsonNode["organisasjonsnummer"].asText()),
        personOppdrag = tilOppdrag(jsonNode["personOppdrag"], jsonNode["fødselsnummer"].asText()),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        if (gjeldendeStatus == Utbetalingsstatus.FORKASTET) person.utbetalingForkastet(utbetalingId)
        this.kommandostarter { utbetalingEndret(this@UtbetalingEndret, sessionContext) }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun toJson(): String = json

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
    oppgaveService: OppgaveService,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
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
                totrinnsvurderingRepository = totrinnsvurderingRepository,
            ),
            OppdaterOppgavestatusCommand(utbetalingId, gjeldendeStatus, oppgaveService),
        )
}

package no.nav.helse.modell.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.InvaliderUtbetalingForGenerasjonerCommand
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao

internal class UtbetalingEndret(
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
    private val json: String
) : Personhendelse {

    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
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
    gjeldendeGenerasjoner: List<Generasjon>,
    utbetalingDao: UtbetalingDao,
    opptegnelseDao: OpptegnelseDao,
    reservasjonDao: ReservasjonDao,
    oppgaveDao: OppgaveDao,
    tildelingDao: TildelingDao,
    oppgaveMediator: OppgaveMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    json: String
): MacroCommand() {
    override val commands: List<Command> = mutableListOf(
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
            opptegnelseDao = opptegnelseDao
        ),
        ReserverPersonHvisTildeltCommand(
            fødselsnummer = fødselsnummer,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            oppgaveDao = oppgaveDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator
        ),
        OppdaterOppgavestatusCommand(utbetalingId, gjeldendeStatus, oppgaveMediator),
    ).apply {
        if (gjeldendeStatus == FORKASTET)
            add(InvaliderUtbetalingForGenerasjonerCommand(utbetalingId, gjeldendeGenerasjoner))
    }

}

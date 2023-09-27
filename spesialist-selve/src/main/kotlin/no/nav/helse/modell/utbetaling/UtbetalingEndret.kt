package no.nav.helse.modell.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.mediator.meldinger.Hendelse
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
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
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
            oppgaveDao = oppgaveDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator
        ),
        OppdaterOppgavestatusCommand(utbetalingId, gjeldendeStatus, oppgaveMediator),
    ).apply {
        if (gjeldendeStatus == FORKASTET)
            add(InvaliderUtbetalingForGenerasjonerCommand(utbetalingId, gjeldendeGenerasjoner))
    }

}

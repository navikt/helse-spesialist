package no.nav.helse.modell.gosysoppgaver

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao

internal class GosysOppgaveEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    sykefraværstilfelle: Sykefraværstilfelle,
    private val json: String,
    gosysOppgaveEndretCommandData: GosysOppgaveEndretCommandData,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    oppgaveMediator: OppgaveMediator,
    oppgaveDao: OppgaveDao,
    utbetalingDao: UtbetalingDao,
    tildelingDao: TildelingDao,
) : Kommandohendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    private val utbetaling = utbetalingDao.hentUtbetaling(gosysOppgaveEndretCommandData.utbetalingId)

    private val oppgaveId by lazy { oppgaveDao.finnOppgaveId(fødselsnummer) }
    private val harTildeltOppgave = oppgaveId?.let { oppgaveId ->
        tildelingDao.tildelingForOppgave(oppgaveId) != null
    } ?: false

    override val commands: List<Command> = listOf(
        ÅpneGosysOppgaverCommand(
            hendelseId = id,
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveMediator = oppgaveMediator,
            vedtaksperiodeId = gosysOppgaveEndretCommandData.vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle,
            harTildeltOppgave = harTildeltOppgave,
        ),
        SjekkAtOppgaveFortsattErÅpenCommand(fødselsnummer = fødselsnummer, oppgaveDao = oppgaveDao),
        SettTidligereAutomatiseringInaktivCommand(
            vedtaksperiodeId = gosysOppgaveEndretCommandData.vedtaksperiodeId,
            hendelseId = gosysOppgaveEndretCommandData.hendelseId,
            automatisering = automatisering,
        ),
        AutomatiseringForEksisterendeOppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = gosysOppgaveEndretCommandData.vedtaksperiodeId,
            hendelseId = gosysOppgaveEndretCommandData.hendelseId,
            automatisering = automatisering,
            godkjenningsbehovJson = gosysOppgaveEndretCommandData.godkjenningsbehovJson,
            godkjenningMediator = godkjenningMediator,
            oppgaveMediator = oppgaveMediator,
            utbetaling = utbetaling,
            periodetype = gosysOppgaveEndretCommandData.periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
        )
    )

}

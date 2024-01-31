package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommandData
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.UtbetalingDao

internal class TilbakedateringGodkjent(
    override val id: UUID,
    private val fødselsnummer: String,
    sykefraværstilfelle: Sykefraværstilfelle,
    private val json: String,
    oppgaveDataForAutomatisering: GosysOppgaveEndretCommandData,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    oppgaveMediator: OppgaveMediator,
    utbetalingDao: UtbetalingDao,
    oppgaveDao: OppgaveDao
) : Kommandohendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    private val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)

    override val commands: List<Command> = listOf(
        SjekkAtOppgaveFortsattErÅpenCommand(fødselsnummer = fødselsnummer, oppgaveDao = oppgaveDao),
        DeaktiverVarselCommand(
            sykefraværstilfelle = sykefraværstilfelle,
            vedtaksperiodeIder = sykefraværstilfelle.alleTilbakedaterteVedtaksperioder(oppgaveDataForAutomatisering.vedtaksperiodeId),
            varselkode = "RV_SØ_3"
        ),
        SettTidligereAutomatiseringInaktivCommand(
            vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId,
            hendelseId = oppgaveDataForAutomatisering.hendelseId,
            automatisering = automatisering,
        ),
        AutomatiseringForEksisterendeOppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId,
            hendelseId = oppgaveDataForAutomatisering.hendelseId,
            automatisering = automatisering,
            godkjenningsbehovJson = oppgaveDataForAutomatisering.godkjenningsbehovJson,
            godkjenningMediator = godkjenningMediator,
            oppgaveMediator = oppgaveMediator,
            utbetaling = utbetaling,
            periodetype = oppgaveDataForAutomatisering.periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
        )
    )

}

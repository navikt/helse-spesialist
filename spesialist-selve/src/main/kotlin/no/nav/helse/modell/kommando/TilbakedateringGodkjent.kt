package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling

internal class TilbakedateringGodkjent(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String
) : Personhendelse {
    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json
}

internal class TilbakedateringGodkjentCommand(
    fødselsnummer: String,
    sykefraværstilfelle: Sykefraværstilfelle,
    utbetaling: Utbetaling,
    automatisering: Automatisering,
    oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
    oppgaveDao: OppgaveDao,
    oppgaveMediator: OppgaveMediator,
    godkjenningMediator: GodkjenningMediator,
): MacroCommand() {
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

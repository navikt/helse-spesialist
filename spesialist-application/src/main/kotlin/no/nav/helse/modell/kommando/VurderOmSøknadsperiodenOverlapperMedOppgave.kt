package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.spesialist.domain.Periode

internal class VurderOmSøknadsperiodenOverlapperMedOppgave(
    private val oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
    private val søknadsperioder: List<Periode>,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        if (!oppgaveDataForAutomatisering.periodeOverlapperMed(søknadsperioder)) return ferdigstill(context)
        return true
    }
}

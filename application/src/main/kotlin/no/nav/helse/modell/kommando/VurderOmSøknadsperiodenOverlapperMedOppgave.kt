package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SpleisBehandlingId

internal class VurderOmSøknadsperiodenOverlapperMedOppgave(
    private val sessionContext: SessionContext,
    private val oppgave: Oppgave,
    private val søknadsperioder: List<Periode>,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val behandling =
            sessionContext.behandlingRepository.finn(SpleisBehandlingId(oppgave.behandlingId))
                ?: error("Fant ikke behandling")
        val søknadOverlapperMedBehandlingTilGodkjenning = søknadsperioder.any { it.overlapper(Periode(behandling.fom, behandling.tom)) }
        if (!søknadOverlapperMedBehandlingTilGodkjenning) return ferdigstill(context)
        return true
    }
}

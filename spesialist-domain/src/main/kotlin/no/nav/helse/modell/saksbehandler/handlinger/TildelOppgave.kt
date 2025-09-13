package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class TildelOppgave(
    oppgaveId: Long,
    private val saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
) : Oppgavehandling(oppgaveId) {
    override fun loggnavn(): String = "tildel_oppgave"

    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {
        oppgave.forsøkTildeling(saksbehandlerWrapper, saksbehandlerTilgangsgrupper)
    }
}

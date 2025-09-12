package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class TildelOppgave(
    oppgaveId: Long,
    private val saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
) : Oppgavehandling(oppgaveId) {
    override fun loggnavn(): String = "tildel_oppgave"

    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        oppgave.forsøkTildeling(legacySaksbehandler, saksbehandlerTilgangsgrupper)
    }
}

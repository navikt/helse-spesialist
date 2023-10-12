package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

class TildelOppgave(oppgaveId: Long): Oppgavehandling(oppgaveId) {
    override fun loggnavn(): String = "tildel_oppgave"

    override fun utførAv(saksbehandler: Saksbehandler) {
        oppgave.forsøkTildeling(saksbehandler)
    }
}
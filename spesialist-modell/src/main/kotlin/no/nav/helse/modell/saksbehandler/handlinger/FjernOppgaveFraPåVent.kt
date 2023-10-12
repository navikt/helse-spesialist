package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

class FjernOppgaveFraPåVent(oppgaveId: Long): Oppgavehandling(oppgaveId) {
    override fun utførAv(saksbehandler: Saksbehandler) {
        oppgave.leggPåVent(saksbehandler)
    }

    override fun loggnavn(): String = "avmeld_oppgave"
}
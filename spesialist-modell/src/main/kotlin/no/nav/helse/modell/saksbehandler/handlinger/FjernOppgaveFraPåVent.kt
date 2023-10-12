package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

class FjernOppgaveFraPåVent(oppgaveId: Long): Oppgavehandling(oppgaveId) {
    override fun utførAv(saksbehandler: Saksbehandler) {
        oppgave.fjernPåVent(saksbehandler)
    }

    override fun loggnavn(): String = "fjern_oppgave_fra_på_vent"
}
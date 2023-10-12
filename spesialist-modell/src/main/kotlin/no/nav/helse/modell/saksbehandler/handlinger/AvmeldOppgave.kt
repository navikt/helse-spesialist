package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

class AvmeldOppgave(oppgaveId: Long): Oppgavehandling(oppgaveId) {
    override fun utførAv(saksbehandler: Saksbehandler) {
        oppgave.forsøkAvmelding(saksbehandler)
    }

    override fun loggnavn(): String = "avmeld_oppgave"
}
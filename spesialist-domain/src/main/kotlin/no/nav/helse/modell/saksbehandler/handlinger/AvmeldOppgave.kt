package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

class AvmeldOppgave(oppgaveId: Long) : Oppgavehandling(oppgaveId) {
    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        oppgave.forsøkAvmelding(legacySaksbehandler)
    }

    override fun loggnavn(): String = "avmeld_oppgave"
}

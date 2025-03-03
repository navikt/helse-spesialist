package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

class TildelOppgave(oppgaveId: Long) : Oppgavehandling(oppgaveId) {
    override fun loggnavn(): String = "tildel_oppgave"

    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        oppgave.forsøkTildeling(legacySaksbehandler)
    }
}

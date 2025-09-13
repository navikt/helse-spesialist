package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper

class AvmeldOppgave(
    oppgaveId: Long,
) : Oppgavehandling(oppgaveId) {
    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {
        oppgave.forsøkAvmelding(saksbehandlerWrapper)
    }

    override fun loggnavn(): String = "avmeld_oppgave"
}

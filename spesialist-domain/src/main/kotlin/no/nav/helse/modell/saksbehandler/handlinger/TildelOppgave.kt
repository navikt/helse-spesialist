package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle

class TildelOppgave(
    oppgaveId: Long,
    private val brukerroller: Set<Brukerrolle>,
) : Oppgavehandling(oppgaveId) {
    override fun loggnavn(): String = "tildel_oppgave"

    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {
        oppgave.forsøkTildeling(saksbehandlerWrapper, brukerroller)
    }
}

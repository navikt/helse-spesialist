package no.nav.helse.tildeling

import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import java.util.*

internal class TildelingMediator(private val saksbehandlerDao: SaksbehandlerDao, private val tildelingDao: TildelingDao) {

    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
        epostadresse: String,
        navn: String
    ) {
        val saksbehandlerFor = tildelingDao.finnSaksbehandlerNavn(oppgaveId)
        if (saksbehandlerFor != null) {
            throw ModellFeil(OppgaveErAlleredeTildelt(saksbehandlerFor))
        }
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerreferanse, navn, epostadresse)
        tildelingDao.opprettTildeling(oppgaveId, saksbehandlerreferanse)
    }

    internal fun fjernTildeling(oppgaveId: Long) = tildelingDao.slettTildeling(oppgaveId)
}

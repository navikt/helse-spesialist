package no.nav.helse.spesialist.db.repository

import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.db.dao.PgOppgaveDao
import no.nav.helse.spesialist.db.dao.PgTildelingDao

class PgOppgaveRepository(
    private val oppgaveDao: PgOppgaveDao,
    private val tildelingDao: PgTildelingDao,
) : OppgaveRepository {
    override fun lagre(oppgave: Oppgave) {
        oppgaveDao.opprettOppgave(oppgave = oppgave)
        oppdaterTildeling(oppgave)
    }

    override fun oppdater(oppgave: Oppgave) {
        oppgaveDao.updateOppgave(oppgave = oppgave)
        oppdaterTildeling(oppgave)
    }

    override fun oppgave(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave {
        val oppgave =
            oppgaveDao
                .finnOppgave(id, tilgangskontroll)
                ?: error("Forventer å finne oppgave med oppgaveId=$id")
        return oppgave
    }

    private fun oppdaterTildeling(oppgave: Oppgave) {
        val tildeltTil = oppgave.tildeltTil
        if (tildeltTil != null) {
            tildelingDao.tildel(oppgave.id, tildeltTil.oid)
        } else {
            tildelingDao.avmeld(oppgave.id)
        }
    }
}

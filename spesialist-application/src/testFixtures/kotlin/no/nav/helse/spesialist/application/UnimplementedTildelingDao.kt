package no.nav.helse.spesialist.application

import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TildelingDto

class UnimplementedTildelingDao : TildelingDao {
    override fun tildelingForPerson(fødselsnummer: String): TildelingDto? {
        TODO("Not yet implemented")
    }

    override fun tildelingForOppgave(oppgaveId: Long): TildelingDto? {
        TODO("Not yet implemented")
    }
}

package no.nav.helse.spesialist.application

import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto

class InMemoryTildelingApiDao : TildelingApiDao {
    override fun tildelingForPerson(fødselsnummer: String): TildelingApiDto? {
        TODO("Not yet implemented")
    }
}

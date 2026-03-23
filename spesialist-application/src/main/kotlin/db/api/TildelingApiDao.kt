package no.nav.helse.db.api

import no.nav.helse.spesialist.api.tildeling.TildelingApiDto

interface TildelingApiDao {
    fun tildelingForPerson(fødselsnummer: String): TildelingApiDto?
}

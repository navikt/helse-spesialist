package no.nav.helse.db

interface TildelingDao {
    fun tildelingForPerson(fødselsnummer: String): TildelingDto?
}

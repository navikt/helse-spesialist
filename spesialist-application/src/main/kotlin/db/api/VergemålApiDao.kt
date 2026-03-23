package no.nav.helse.db.api

interface VergemålApiDao {
    fun harFullmakt(fødselsnummer: String): Boolean?
}

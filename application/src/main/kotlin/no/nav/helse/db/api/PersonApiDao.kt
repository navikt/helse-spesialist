package no.nav.helse.db.api

interface PersonApiDao {
    fun finnInfotrygdutbetalinger(fødselsnummer: String): String?
}

package no.nav.helse.db.api

interface EgenAnsattApiDao {
    fun erEgenAnsatt(fødselsnummer: String): Boolean?
}

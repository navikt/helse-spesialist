package no.nav.helse.db.api

interface PersonApiDao {
    fun finnInfotrygdutbetalinger(fødselsnummer: String): String?

    fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean

    fun finnAktørId(fødselsnummer: String): String

    fun finnFødselsnumre(aktørId: String): List<String>

    fun harDataNødvendigForVisning(fødselsnummer: String): Boolean
}

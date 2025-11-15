package no.nav.helse.spesialist.application

import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto

class InMemoryPersonApiDao : PersonApiDao {
    override fun personKlargjøres(fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    override fun klargjøringPågår(fødselsnummer: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun finnEnhet(fødselsnummer: String): EnhetDto {
        TODO("Not yet implemented")
    }

    override fun finnInfotrygdutbetalinger(fødselsnummer: String): String? {
        TODO("Not yet implemented")
    }

    override fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun hentAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? {
        TODO("Not yet implemented")
    }

    override fun finnAktørId(fødselsnummer: String): String {
        TODO("Not yet implemented")
    }

    override fun finnFødselsnumre(aktørId: String): List<String> {
        TODO("Not yet implemented")
    }

    override fun harDataNødvendigForVisning(fødselsnummer: String): Boolean {
        TODO("Not yet implemented")
    }
}

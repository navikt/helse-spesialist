package no.nav.helse.db.api

import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto

interface PartialPersonApiDao : PersonApiDao {
    override fun personKlargjøres(fødselsnummer: String) {
        error("Not implemented for this test")
    }

    override fun klargjøringPågår(fødselsnummer: String): Boolean {
        error("Not implemented for this test")
    }

    override fun finnEnhet(fødselsnummer: String): EnhetDto {
        error("Not implemented for this test")
    }

    override fun finnInfotrygdutbetalinger(fødselsnummer: String): String? {
        error("Not implemented for this test")
    }

    override fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean {
        error("Not implemented for this test")
    }

    override fun hentAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? {
        error("Not implemented for this test")
    }

    override fun finnAktørId(fødselsnummer: String): String {
        error("Not implemented for this test")
    }

    override fun finnFødselsnumre(aktørId: String): List<String> {
        error("Not implemented for this test")
    }

    override fun harDataNødvendigForVisning(fødselsnummer: String): Boolean {
        error("Not implemented for this test")
    }

}

package no.nav.helse.db.api

import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto

interface PersonApiDao {
    fun personKlargjøres(fødselsnummer: String)

    fun klargjøringPågår(fødselsnummer: String): Boolean

    fun finnEnhet(fødselsnummer: String): EnhetDto

    fun finnInfotrygdutbetalinger(fødselsnummer: String): String?

    fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean

    fun personHarAdressebeskyttelse(
        fødselsnummer: String,
        adressebeskyttelse: Adressebeskyttelse,
    ): Boolean

    fun finnAktørId(fødselsnummer: String): String

    fun finnFødselsnumre(aktørId: String): List<String>

    fun harDataNødvendigForVisning(fødselsnummer: String): Boolean
}

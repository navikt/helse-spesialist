package no.nav.helse.db.api

import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Identitetsnummer

interface PersonApiDao {
    fun personKlargjøres(fødselsnummer: String)

    fun klargjøringPågår(fødselsnummer: String): Boolean

    fun finnEnhet(fødselsnummer: String): EnhetDto

    fun finnInfotrygdutbetalinger(fødselsnummer: String): String?

    fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean

    fun hentAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse?

    fun finnAktørId(fødselsnummer: String): String

    fun finnFødselsnumre(aktørId: String): List<String>

    fun finnAndreFødselsnumre(fødselsnummer: String): List<Pair<Identitetsnummer, PersonPseudoId>>

    fun harDataNødvendigForVisning(fødselsnummer: String): Boolean
}

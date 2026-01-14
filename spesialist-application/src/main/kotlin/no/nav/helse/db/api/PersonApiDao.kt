package no.nav.helse.db.api

import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto

interface PersonApiDao {
    fun finnEnhet(fødselsnummer: String): EnhetDto

    fun finnInfotrygdutbetalinger(fødselsnummer: String): String?

    fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean

    fun finnAktørId(fødselsnummer: String): String

    fun finnFødselsnumre(aktørId: String): List<String>

    fun harDataNødvendigForVisning(fødselsnummer: String): Boolean
}

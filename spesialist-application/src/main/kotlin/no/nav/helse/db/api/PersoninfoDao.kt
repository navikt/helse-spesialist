package no.nav.helse.db.api

import java.time.LocalDate

interface PersoninfoDao {
    fun hentPersoninfo(fødselsnummer: String): Personinfo?

    data class Personinfo(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val fodselsdato: LocalDate,
        val kjonn: Kjonn,
        val adressebeskyttelse: Adressebeskyttelse,
    ) {
        enum class Kjonn {
            Kvinne,
            Mann,
            Ukjent,
        }

        enum class Adressebeskyttelse {
            Ugradert,
            Fortrolig,
            StrengtFortrolig,
            StrengtFortroligUtland,
            Ukjent,
        }
    }
}

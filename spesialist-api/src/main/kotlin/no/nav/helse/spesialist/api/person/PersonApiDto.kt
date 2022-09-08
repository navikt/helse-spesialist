package no.nav.helse.spesialist.api.person

import java.time.LocalDate

data class PersoninfoApiDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?,
    val adressebeskyttelse: Adressebeskyttelse
)

enum class Kjønn { Mann, Kvinne, Ukjent }

enum class Adressebeskyttelse {
    Ugradert,
    Fortrolig,
    StrengtFortrolig,
    StrengtFortroligUtland,
    Ukjent
}

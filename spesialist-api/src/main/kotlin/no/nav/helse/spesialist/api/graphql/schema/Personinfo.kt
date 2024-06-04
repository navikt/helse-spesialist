package no.nav.helse.spesialist.api.graphql.schema

import java.time.LocalDate

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

data class Personinfo(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fodselsdato: LocalDate?,
    val kjonn: Kjonn,
    val adressebeskyttelse: Adressebeskyttelse,
    val reservasjon: Reservasjon? = null,
    val unntattFraAutomatisering: UnntattFraAutomatiskGodkjenning? = null,
)

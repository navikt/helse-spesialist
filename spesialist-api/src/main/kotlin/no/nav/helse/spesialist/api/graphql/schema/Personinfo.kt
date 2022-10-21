package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spesialist.api.person.Kjønn

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
    Ukjent
}

data class Personinfo(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fodselsdato: DateString?,
    val kjonn: Kjonn,
    val adressebeskyttelse: Adressebeskyttelse,
    val reservasjon: Reservasjon? = null,
)

internal fun Kjønn.tilKjonn(): Kjonn = when (this) {
    Kjønn.Mann -> Kjonn.Mann
    Kjønn.Kvinne -> Kjonn.Kvinne
    Kjønn.Ukjent -> Kjonn.Ukjent
}

internal fun no.nav.helse.spesialist.api.person.Adressebeskyttelse.tilAdressebeskyttelse(): Adressebeskyttelse = when (this) {
    no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert -> Adressebeskyttelse.Ugradert
    no.nav.helse.spesialist.api.person.Adressebeskyttelse.Fortrolig -> Adressebeskyttelse.Fortrolig
    no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortrolig -> Adressebeskyttelse.StrengtFortrolig
    no.nav.helse.spesialist.api.person.Adressebeskyttelse.StrengtFortroligUtland -> Adressebeskyttelse.StrengtFortroligUtland
    no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ukjent -> Adressebeskyttelse.Ukjent
}
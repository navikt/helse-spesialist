package no.nav.helse.spesialist.client.speed.dto

import java.time.LocalDate

internal data class HistoriskeIdenterRequest(
    val ident: String,
)

internal data class HistoriskeIdenterResponse(
    val fødselsnumre: List<String>,
    val kilde: String,
)

internal data class PersonRequest(
    val ident: String,
)

internal data class PersonResponse(
    val fødselsdato: LocalDate,
    val dødsdato: LocalDate?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val adressebeskyttelse: Adressebeskyttelse,
    val kjønn: Kjønn,
    val kilde: String,
) {
    enum class Adressebeskyttelse {
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND,
        UGRADERT,
    }

    enum class Kjønn {
        MANN,
        KVINNE,
        UKJENT,
    }
}

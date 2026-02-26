package no.nav.helse.spesialist.client.speed.dto

import java.time.LocalDate

internal data class AlleIdenterRequest(
    val ident: String,
)

internal data class AlleIdenterResponse(
    val identer: List<Ident>,
    val kilde: String,
) {
    data class Ident(
        val ident: String,
        val type: IdentType,
        val gjeldende: Boolean,
    )

    enum class IdentType {
        FOLKEREGISTERIDENT,
        AKTORID,
        NPID,
    }
}

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

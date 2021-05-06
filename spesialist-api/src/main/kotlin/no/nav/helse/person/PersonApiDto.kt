package no.nav.helse.person

import java.time.LocalDate

data class PersoninfoApiDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?
)

enum class Kjønn { Mann, Kvinne, Ukjent }

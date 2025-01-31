package no.nav.helse.util

import java.time.LocalDate
import java.util.UUID

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class Enhet(
    val id: Int,
    val navn: String,
)

data class Periode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
) {
    companion object {
        infix fun LocalDate.til(tom: LocalDate) =
            Periode(UUID.randomUUID(), this, tom)
    }
}

data class Arbeidsforhold(
    val start: LocalDate,
    val slutt: LocalDate,
    val tittel: String,
    val prosent: Int,
)

data class Saksbehandler(
    val oid: UUID,
    val navn: String,
    val ident: String,
    val epost: String,
)

@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiPerson(
    val identitetsnummer: String,
    val andreIdentitetsnumre: List<String>,
    val aktørId: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val dødsdato: LocalDate?,
    val kjønn: Kjønn,
    val adressebeskyttelse: Adressebeskyttelse,
) {
    enum class Kjønn {
        KVINNE,
        MANN,
        UKJENT,
    }

    enum class Adressebeskyttelse {
        UGRADERT,
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND,
        UKJENT,
    }
}

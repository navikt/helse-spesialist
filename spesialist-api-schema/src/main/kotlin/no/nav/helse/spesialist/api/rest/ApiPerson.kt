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
    val etternavn: String,
    val kjønn: Kjønn,
    val alder: Int,
    val boenhet: Boenhet,
) {
    enum class Kjønn {
        KVINNE,
        MANN,
        UKJENT,
    }

    @Serializable
    data class Boenhet(
        val enhetNr: String,
    )
}

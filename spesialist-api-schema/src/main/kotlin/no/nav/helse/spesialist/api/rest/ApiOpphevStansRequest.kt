@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import io.github.smiley4.schemakenerator.core.annotations.Name
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
@Name("OpphevStansRequest")
data class ApiOpphevStansRequest(
    val fodselsnummer: String,
    val begrunnelse: String,
)

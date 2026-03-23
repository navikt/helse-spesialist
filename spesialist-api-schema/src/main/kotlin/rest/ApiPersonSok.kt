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
data class ApiPersonSokRequest(
    val aktørId: String?,
    val identitetsnummer: String?,
)

@Serializable
data class ApiPersonSokResponse(
    val personPseudoId: UUID,
    val klarForVisning: Boolean,
)

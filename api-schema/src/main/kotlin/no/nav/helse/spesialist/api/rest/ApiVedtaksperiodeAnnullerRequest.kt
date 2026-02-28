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
data class ApiVedtaksperiodeAnnullerRequest(
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val årsaker: List<Årsak>,
    val kommentar: String?,
) {
    @Serializable
    data class Årsak(
        val key: String,
        val årsak: String,
    )
}

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
enum class ApiKrrRegistrertStatus {
    RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING,
    IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING,
    IKKE_REGISTRERT_I_KRR,
}

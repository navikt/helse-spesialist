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
class ApiVarsel(
    val id: UUID,
    val definisjonId: UUID,
    val opprettet: LocalDateTime,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val status: ApiVarselstatus,
    val vurdering: ApiVarselvurdering?,
) {
    @Serializable
    data class ApiVarselvurdering(
        val ident: String,
        val tidsstempel: LocalDateTime,
    )

    @Serializable
    enum class ApiVarselstatus {
        AKTIV,
        VURDERT,
        GODKJENT,
    }
}

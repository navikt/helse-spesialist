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
sealed interface ApiPatchEndring<T> {
    val fra: T
    val til: T
}

@Serializable
data class ApiPatchStringEndring(
    override val fra: String,
    override val til: String,
) : ApiPatchEndring<String>

@Serializable
data class ApiPatchDatoPeriodeEndring(
    override val fra: ApiDatoPeriode,
    override val til: ApiDatoPeriode,
) : ApiPatchEndring<ApiDatoPeriode>

@Serializable
data class ApiPatchBigDecimalEndring(
    override val fra: BigDecimal,
    override val til: BigDecimal,
) : ApiPatchEndring<BigDecimal>

@Serializable
data class ApiPatchListLocalDateEndring(
    override val fra: List<LocalDate>,
    override val til: List<LocalDate>,
) : ApiPatchEndring<List<LocalDate>>

@Serializable
data class ApiPatchBooleanEndring(
    override val fra: Boolean,
    override val til: Boolean,
) : ApiPatchEndring<Boolean>

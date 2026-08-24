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
import java.util.*

@Serializable
data class ApiLeggTilGraderteAndreYtelserRequest(
    val fodselsnummer: String,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelseType: ApiGraderteAndreYtelseType,
    val notatTilBeslutter: String,
)

@Serializable
enum class ApiGraderteAndreYtelseType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}

@Serializable
data class ApiGraderteAndreYtelserPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
)

@Serializable
data class ApiLeggTilGraderteAndreYtelserResponse(
    val andreYtelserId: UUID,
)

@Serializable
data class ApiGraderteAndreYtelser(
    val andreYtelserId: UUID,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelseType: ApiGraderteAndreYtelseType,
)

@Serializable
data class ApiPatchGraderteAndreYtelserRequest(
    val graderteAndreYtelserId: UUID,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelseType: ApiGraderteAndreYtelseType,
    val notatTilBeslutter: String,
)

@Serializable
data class ApiPatchGraderteAndreYtelserResponse(
    val andreYtelserId: UUID,
)

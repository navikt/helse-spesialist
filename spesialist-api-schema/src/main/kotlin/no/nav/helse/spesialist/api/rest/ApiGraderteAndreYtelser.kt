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
data class ApiEndreGraderteAndreYtelserRequest(
    val yrkesaktivitet: ApiYrkesaktivitetstype,
    val organisasjonsnummer: String?,
    val versjon: UUID?,
    val dager: List<ApiGraderteAndreYtelserDag>,
    val notatTilBeslutter: String,
)

typealias Grad = Int

@Serializable
data class ApiGraderteAndreYtelserDag(
    val dato: LocalDate,
    val andreYtelserGrader: Map<ApiGraderteAndreYtelserType, Grad>,
    val sykdomsgrad: Grad,
    val arbeidsgrad: Grad,
)

@Serializable
enum class ApiGraderteAndreYtelserType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}

@Serializable
enum class ApiYrkesaktivitetstype {
    ARBEIDSTAKER,
    FRILANS,
    ARBEIDSLEDIG,
    SELVSTENDIG,
}

@Serializable
data class ApiEndreGraderteAndreYtelserResponse(
    val versjon: UUID,
)

@Serializable
data class ApiGraderteAndreYtelser(
    val yrkesaktivitet: ApiYrkesaktivitetstype,
    val organisasjonsnummer: String?,
    val versjon: UUID,
    val dager: List<ApiGraderteAndreYtelserDag>,
    val events: List<ApiGraderteAndreYtelserEndretEvent>,
)

@Serializable
data class ApiGraderteAndreYtelserEndretEvent(
    val versjon: UUID,
    val tidspunkt: LocalDateTime,
    val utfortAvSaksbehandlerIdent: String,
    val notatTilBeslutter: String,
    val dager: List<ApiGraderteAndreYtelserDag>,
)

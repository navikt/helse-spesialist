@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiNotatRequest(
    val tekst: String,
)

@Serializable
data class ApiNotatResponse(
    val id: Int,
)

@Serializable
data class ApiNotat(
    val id: Int,
    val dialogRef: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerEpost: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: UUID,
    val feilregistrert: Boolean,
    val feilregistrert_tidspunkt: LocalDateTime?,
    val type: ApiNotatType,
    val kommentarer: List<ApiKommentar>,
)

enum class ApiNotatType {
    Retur,
    Generelt,
    PaaVent,
    OpphevStans,
}

@Serializable
data class ApiKommentar(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
    val feilregistrert_tidspunkt: LocalDateTime?,
)

@Serializable
data class ApiKommentarRequest(
    val tekst: String,
)

@Serializable
data class ApiKommentarResponse(
    val id: Int,
)

@Serializable
data class ApiPutNotatRequest(
    val feilregistrert: Boolean,
)
